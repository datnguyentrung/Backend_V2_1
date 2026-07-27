package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.domain.Core.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Core.Coach;
import com.dat.ai_receptionist_web.domain.Core.Student;
import com.dat.ai_receptionist_web.domain.Operation.ClassSession;
import com.dat.ai_receptionist_web.domain.Operation.StudentAttendance;
import com.dat.ai_receptionist_web.domain.Operation.StudentEnrollment;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Security.UserProfile;
import com.dat.ai_receptionist_web.dto.Operation.CheckInStudentProjection;
import com.dat.ai_receptionist_web.dto.Operation.StudentAttendanceDTO;
import com.dat.ai_receptionist_web.dto.Operation.StudentEnrollmentResDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.ErrorCode;
import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.enums.Core.StudentStatus;
import com.dat.ai_receptionist_web.enums.Operation.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Operation.EvaluationStatus;
import com.dat.ai_receptionist_web.enums.Operation.NotificationType;
import com.dat.ai_receptionist_web.enums.Operation.SessionStatus;
import com.dat.ai_receptionist_web.enums.Operation.StudentEnrollmentStatus;
import com.dat.ai_receptionist_web.event.ScoreRecalculateEvent;
import com.dat.ai_receptionist_web.mapper.Operation.StudentAttendanceMapper;
import com.dat.ai_receptionist_web.repository.Core.CoachRepository;
import com.dat.ai_receptionist_web.repository.Core.StudentRepository;
import com.dat.ai_receptionist_web.repository.Operation.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Operation.StudentAttendanceRepository;
import com.dat.ai_receptionist_web.repository.Operation.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Core.CoachService;
import com.dat.ai_receptionist_web.service.Core.StudentService;
import com.dat.ai_receptionist_web.service.Security.AuthTokenService;
import com.dat.ai_receptionist_web.service.Security.UserProfileService;
import com.dat.ai_receptionist_web.specification.StudentAttendanceSpecification;
import com.dat.ai_receptionist_web.util.error.AppException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentAttendanceService {
    private static final String ATTENDANCE_UNIQUE_CONSTRAINT =
            "uk_student_attendance_enrollment_session";

    private final StudentAttendanceRepository studentAttendanceRepository;
    private final CoachService coachService;
    private final StudentAttendanceMapper studentAttendanceMapper;
    private final StudentEnrollmentService studentEnrollmentService;
    private final NotificationService notificationService;
    private final AuthTokenService authTokenService;
    private final StudentService studentService;
    private final CoachRepository coachRepository;
    private final ClassSessionRepository classSessionRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AttendanceNotificationTaskExecutor attendanceNotificationTaskExecutor;
    private final AttendanceNotificationDispatcher attendanceNotificationDispatcher;

    @Autowired
    @Lazy
    private StudentAttendanceService self; // Self-injection để gọi method có @Transactional từ cùng class
    @Autowired
    private UserProfileService userProfileService;

    private void publishScoreRecalculateEvent(Student student, LocalDate sessionDate) {
        if (student == null || sessionDate == null) return;

        int year = sessionDate.getYear();
        int quarter = (sessionDate.getMonthValue() - 1) / 3 + 1; // Công thức tính Quý

        ScoreRecalculateEvent event = new ScoreRecalculateEvent(
                student.getStudentCode(),
                quarter,
                year
        );
        runAfterCommit(() -> eventPublisher.publishEvent(event));
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runNotificationAction(action);
                }
            });
            return;
        }

        runNotificationAction(action);
    }

    private void runNotificationAction(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("Failed to run after-commit action", e);
        }
    }

    private void enqueueAttendanceNotificationAfterCommit(UUID attendanceId) {
        runAfterCommit(() -> attendanceNotificationTaskExecutor.submit(
                attendanceId,
                () -> attendanceNotificationDispatcher.dispatch(attendanceId)
        ));
    }

    @Transactional
//    @Caching(evict = {
//            // Khi điểm danh thay đổi, nếu có thống kê đi học trong chi tiết Học viên/Lớp thì phải xóa
//            //@CacheEvict(value = "studentDetail", allEntries = true),
//            //@CacheEvict(value = "classScheduleDetail", allEntries = true)
//    })
    public List<StudentAttendanceDTO.Response> updateStudentAttendance(
            List<StudentAttendanceDTO.SimpleResponse> requests,
            String coachId) { // Nhận trực tiếp ID thay vì Object

        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Dùng getReferenceById (Proxy) thay vì findById
        // KHÔNG tạo ra câu lệnh SELECT nào xuống DB, chỉ khởi tạo proxy object để gán Khóa Ngoại.
        Coach coachProxy = coachRepository.getReferenceById(UUID.fromString(coachId));

        // 2. Lấy toàn bộ ID từ request
        Set<UUID> attendanceIds = requests.stream()
                .map(StudentAttendanceDTO.SimpleResponse::getAttendanceId)
                .collect(Collectors.toSet());

        // 3. Tối ưu N+1: Lấy TOÀN BỘ record trong 1 câu Query duy nhất (SELECT ... WHERE id IN (...))
        List<StudentAttendance> existingRecords = studentAttendanceRepository.findAllById(attendanceIds);

        // Kiểm tra xem có bản ghi nào bịa đặt/không tồn tại không
        if (existingRecords.size() != attendanceIds.size()) {
            throw new NoSuchElementException("Một hoặc nhiều bản ghi điểm danh không tồn tại trong hệ thống.");
        }

        // 4. Chuyển List thành Map để tra cứu tốc độ O(1)
        Map<UUID, StudentAttendance> attendanceMap = existingRecords.stream()
                .collect(Collectors.toMap(StudentAttendance::getAttendanceId, a -> a));

        // 5. Cập nhật dữ liệu
        for (StudentAttendanceDTO.SimpleResponse dto : requests) {
            StudentAttendance entity = attendanceMap.get(dto.getAttendanceId());

            // Update Trạng thái điểm danh
            if (!Objects.equals(dto.getAttendanceStatus(), entity.getAttendanceStatus())) {
                entity.setAttendanceStatus(dto.getAttendanceStatus());
                // Ghi nhận thời gian điểm danh thực tế (nếu trước đó là vắng)
                if (entity.getCheckInTime() == null && dto.getAttendanceStatus() != AttendanceStatus.ABSENT) {
                    entity.setCheckInTime(LocalDateTime.now());
                }
                entity.setRecordedByCoach(coachProxy);
            }

            // Update Trạng thái đánh giá
            if (!Objects.equals(dto.getEvaluationStatus(), entity.getEvaluationStatus())) {
                entity.setEvaluationStatus(dto.getEvaluationStatus());
                entity.setEvaluatedByCoach(coachProxy);
            }

            // Update Ghi chú (Dùng Objects.equals để an toàn với null)
            if (!Objects.equals(dto.getNote(), entity.getNote())) {
                entity.setNote(dto.getNote());
            }
        }

        // 6. Lưu tất cả thay đổi chỉ với 1 câu lệnh saveAll (thay vì save từng bản ghi)
        Set<String> processedStudents = new HashSet<>(); // Dùng Set để tránh 1 học sinh bị tính lại 2 lần trong 1
        // request
        for (StudentAttendance entity : existingRecords) {
            String uniqueKey =
                    entity.getStudentEnrollment().getStudent().getStudentCode() + "_" + entity.getSessionDate().getMonthValue();
            if (processedStudents.add(uniqueKey)) {
                publishScoreRecalculateEvent(entity.getStudentEnrollment().getStudent(), entity.getSessionDate());
            }
        }
        return studentAttendanceMapper.toResponseList(existingRecords);
    }

    public List<StudentAttendance> getAttendancesByUserIdAndSessionDate(UUID studentUserId, LocalDate sessionDate) {
        return studentAttendanceRepository
                .findByStudentEnrollment_Student_PersonIdAndSessionDate(studentUserId, sessionDate);
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentAttendanceDTO.Response createAttendanceRecord(StudentAttendanceDTO.CreateRequest request) {
        // 1. Validate Student
        CheckInStudentProjection student = request.getStudentCode() != null ?
                studentRepository.findCheckInStudentByStudentCode(request.getStudentCode())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND))
                : studentRepository.findCheckInStudentByPersonId(request.getPersonId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        return createAttendanceRecordForResolvedStudent(student);
    }

    /**
     * Internal fast path for face check-in. The caller has already resolved the student
     * while identifying the check-in target, so no second student lookup is needed.
     */
    @Transactional(rollbackFor = Exception.class)
    public StudentAttendanceDTO.Response createAttendanceRecordForResolvedStudent(CheckInStudentProjection student) {
        return createAttendanceRecordForResolvedStudentInternal(student);
    }

    private StudentAttendanceDTO.Response createAttendanceRecordForResolvedStudentInternal(CheckInStudentProjection student) {
        if (student.getStudentStatus() != StudentStatus.ACTIVE) {
            throw new AppException(ErrorCode.STUDENT_INACTIVE);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        // 2. Lấy danh sách đăng ký lớp
        List<StudentEnrollment> enrollments = studentEnrollmentRepository
                .findByStudent_StudentCodeAndStatusWithClassSchedule(
                        student.getStudentCode(),
                        StudentEnrollmentStatus.ACTIVE
                );

        if (enrollments.isEmpty()) {
            throw new AppException(ErrorCode.STUDENT_ACTIVE_ENROLLMENT_NOT_FOUND);
        }

        List<String> enrolledScheduleIds = enrollments.stream()
                .map(e -> e.getClassSchedule().getScheduleId())
                .toList();

        List<ClassSession> relevantSessions = classSessionRepository
                .findBySessionDateAndClassSchedule_ScheduleIdIn(today, enrolledScheduleIds);
        ClassSession currentSession = selectAutomaticSession(relevantSessions);
        StudentEnrollment enrollment = enrollments.stream()
                .filter(item -> item.getClassSchedule().getScheduleId()
                        .equals(currentSession.getClassSchedule().getScheduleId()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND));

        StudentEnrollment lockedEnrollment = studentEnrollmentRepository
                .findForCheckInByEnrollmentId(enrollment.getEnrollmentId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_ACTIVE_ENROLLMENT_NOT_FOUND));
        AttendanceStatus newStatus = resolveCheckInStatus(lockedEnrollment, currentTime);

        Optional<StudentAttendance> existingAttendance = studentAttendanceRepository
                .findByStudentEnrollment_EnrollmentIdAndClassSession_SessionId(
                        lockedEnrollment.getEnrollmentId(), currentSession.getSessionId());
        if (existingAttendance.isPresent()) {
            StudentAttendance attendance = existingAttendance.get();
            if (attendance.getCheckInTime() != null) {
                log.info("CHECK_IN_RESULT personId={} attendanceId={} alreadyCheckedIn=true checkInTime={}",
                        student.getPersonId(), attendance.getAttendanceId(), attendance.getCheckInTime());
                return toCheckInResponse(attendance, student, lockedEnrollment, currentSession, true);
            }
            if (!isAutomaticCheckInAllowed(attendance.getAttendanceStatus())) {
                throw new AppException(ErrorCode.ATTENDANCE_CHECK_IN_NOT_ALLOWED);
            }

            AttendanceStatus previousStatus = attendance.getAttendanceStatus();
            attendance.setAttendanceStatus(newStatus);
            attendance.setCheckInTime(now);
            StudentAttendance savedAttendance = studentAttendanceRepository.saveAndFlush(attendance);
            enqueueAttendanceNotificationAfterCommit(savedAttendance.getAttendanceId());
            log.info("CHECK_IN_RESULT personId={} attendanceId={} alreadyCheckedIn=false previousStatus={} newStatus={}",
                    student.getPersonId(), savedAttendance.getAttendanceId(), previousStatus, newStatus);
            return toCheckInResponse(savedAttendance, student, lockedEnrollment, currentSession, false);
        }

        StudentAttendance savedAttendance = studentAttendanceRepository.saveAndFlush(StudentAttendance.builder()
                .studentEnrollment(lockedEnrollment)
                .classSession(currentSession)
                .sessionDate(today)
                .attendanceStatus(newStatus)
                .evaluationStatus(EvaluationStatus.PENDING)
                .checkInTime(now)
                .note("Điểm danh tự động qua API")
                .build());
        enqueueAttendanceNotificationAfterCommit(savedAttendance.getAttendanceId());
        log.info("CHECK_IN_RESULT personId={} attendanceId={} alreadyCheckedIn=false previousStatus=null newStatus={}",
                student.getPersonId(), savedAttendance.getAttendanceId(), newStatus);
        return toCheckInResponse(savedAttendance, student, lockedEnrollment, currentSession, false);
    }

    private static AttendanceStatus resolveCheckInStatus(StudentEnrollment enrollment, LocalTime currentTime) {
        return currentTime.isBefore(enrollment.getClassSchedule().getStartTime())
                ? AttendanceStatus.PRESENT
                : AttendanceStatus.LATE;
    }

    private ClassSession selectAutomaticSession(List<ClassSession> sessions) {
        List<ClassSession> ordered = sessions.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ClassSession::getStartTime)
                        .thenComparing(ClassSession::getSessionId))
                .toList();
        ClassSession selected = ordered.stream()
                .filter(session -> session.getStatus() == SessionStatus.ACTIVE)
                .filter(session -> !session.isAttendanceClosed())
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND));

        boolean hasEarlierUnfinishedSession = ordered.stream()
                .takeWhile(session -> !session.getSessionId().equals(selected.getSessionId()))
                .anyMatch(session -> !isTerminal(session));
        if (hasEarlierUnfinishedSession) {
            throw new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND);
        }

        long sameStartTimeCandidates = ordered.stream()
                .filter(session -> session.getStatus() == SessionStatus.ACTIVE)
                .filter(session -> !session.isAttendanceClosed())
                .filter(session -> session.getStartTime().equals(selected.getStartTime()))
                .count();
        if (sameStartTimeCandidates > 1) {
            throw new AppException(ErrorCode.MULTIPLE_ACTIVE_CLASS_SESSIONS);
        }
        return selected;
    }

    private static boolean isTerminal(ClassSession session) {
        return session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.CANCELLED
                || session.getStatus() == SessionStatus.TERMINATED;
    }

    private static boolean isAutomaticCheckInAllowed(AttendanceStatus attendanceStatus) {
        return attendanceStatus == AttendanceStatus.ABSENT
                || attendanceStatus == AttendanceStatus.PRESENT
                || attendanceStatus == AttendanceStatus.LATE;
    }

    private boolean isDuplicateAttendanceConstraint(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException constraintViolation
                    && ATTENDANCE_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintViolation.getConstraintName())) {
                return true;
            }
            if (cause.getMessage() != null
                    && cause.getMessage().contains(ATTENDANCE_UNIQUE_CONSTRAINT)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * Cập nhật trạng thái điểm danh của học viên.
     * <p>
     * Chức năng: Cho phép HLV cập nhật trạng thái điểm danh (PRESENT, ABSENT, LATE, EXCUSED)
     * và thời gian check-in của một học viên cụ thể.
     * <p>
     * Business Rules:
     * - Chỉ HLV có trạng thái ACTIVE mới được phép thực hiện
     * - Tự động ghi nhận HLV đã thực hiện điểm danh
     * - Cập nhật timestamp để audit trail
     *
     * @param coachId      ID của HLV thực hiện thao tác
     * @param request      Thông tin cập nhật (trạng thái và thời gian check-in)
     * @param attendanceId ID của bản ghi điểm danh cần cập nhật
     * @throws NoSuchElementException nếu không tìm thấy bản ghi điểm danh
     * @throws AccessDeniedException  nếu HLV không ở trạng thái ACTIVE
     */
    @Transactional(rollbackFor = Exception.class)
//    @Caching(evict = {
//            //@CacheEvict(value = "studentDetail", allEntries = true),
//            //@CacheEvict(value = "classScheduleDetail", allEntries = true)
//    })
    public StudentAttendanceDTO.Response updateAttendanceStatus(
            UUID coachId,
            StudentAttendanceDTO.UpdateStatusRequest request,
            UUID attendanceId
    ) {
        Coach currentCoach = coachService.validateCoachAndGetActive(coachId);

        StudentAttendance attendance = studentAttendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Không tìm thấy bản ghi điểm danh với ID: " + attendanceId
                ));

        if (attendance.getAttendanceStatus() == AttendanceStatus.EXCUSED
                || request.getAttendanceStatus() == AttendanceStatus.MAKEUP) {
            throw new IllegalStateException("Không thể thay đổi bản ghi điểm danh đã được xin phép.");
        }

        attendance.setAttendanceStatus(request.getAttendanceStatus());
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setRecordedByCoach(currentCoach);

        if (
                request.getAttendanceStatus() == AttendanceStatus.ABSENT
                        || request.getAttendanceStatus() == AttendanceStatus.EXCUSED
        ) {
            attendance.setCheckInTime(null);
            attendance.setRecordedByCoach(null);
            attendance.setEvaluationStatus(null);
            attendance.setEvaluatedByCoach(null);
            attendance.setNote(null); // nếu có field note/remark thì nên clear luôn
        }

        if (request.getAttendanceStatus() != AttendanceStatus.ABSENT) {
            enqueueAttendanceNotificationAfterCommit(attendance.getAttendanceId());
        }

        publishScoreRecalculateEvent(
                attendance.getStudentEnrollment().getStudent(),
                attendance.getSessionDate()
        );

        return studentAttendanceMapper.toResponse(attendance);
    }

    /**
     * Gửi thông báo đánh giá học viên sau buổi học.
     * <p>
     * Chức năng: Thông báo cho học viên/phụ huynh về kết quả đánh giá sau buổi học
     * kèm theo ghi chú của HLV nếu có.
     *
     * @param attendance Bản ghi điểm danh đã có thông tin đánh giá
     */
    private void sendEvaluationNotification(
            StudentAttendance attendance
    ) {
        // Lấy thông tin học viên và HLV từ attendance
        Student student = attendance.getStudentEnrollment().getStudent();
        Coach coach = attendance.getEvaluatedByCoach();

        if (coach == null) {
            log.warn("Cannot send evaluation notification: no coach recorded for attendance {}",
                    attendance.getAttendanceId());
            return;
        }

        // Lấy thông tin lịch học
        String scheduleId = attendance.getStudentEnrollment().getClassSchedule().getScheduleId();

        // Format ngày học
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
        String formattedDate = attendance.getSessionDate().format(dateFormatter);

        // Xây dựng nội dung dựa trên trạng thái đánh giá
        String title;
        String body;
        StringBuilder bodyBuilder = new StringBuilder();

        String format = String.format("🏫 Cơ sở: %s (ca %s)\n",
                scheduleId.charAt(1), scheduleId.charAt(4));
        switch (attendance.getEvaluationStatus()) {
            case GOOD:
                title = "⭐ Đánh giá: TỐT";
                bodyBuilder.append(String.format("HV %s được HLV %s đánh giá TỐT sau buổi học.\n",
                        student.getFullName(), coach.getFullName()));
                bodyBuilder.append(format);
                bodyBuilder.append(String.format("📅 Ngày: %s", formattedDate));
                break;
            case AVERAGE:
                title = "📊 Đánh giá: TRUNG BÌNH";
                bodyBuilder.append(String.format("HV %s được HLV %s đánh giá TRUNG BÌNH sau buổi học.\n",
                        student.getFullName(), coach.getFullName()));
                bodyBuilder.append(format);
                bodyBuilder.append(String.format("📅 Ngày: %s", formattedDate));
                break;
            case WEAK:
                title = "⚠️ Đánh giá: CẦN CỐ GẮNG";
                bodyBuilder.append(String.format("HV %s được HLV %s đánh giá CẦN CỐ GẮNG HƠN sau buổi học.\n",
                        student.getFullName(), coach.getFullName()));
                bodyBuilder.append(format);
                bodyBuilder.append(String.format("📅 Ngày: %s", formattedDate));
                break;
            case PENDING:
            default:
                // Không gửi thông báo cho trạng thái PENDING
                return;
        }

        // Thêm ghi chú nếu có
        if (attendance.getNote() != null && !attendance.getNote().trim().isEmpty()) {
            bodyBuilder.append(String.format("\n💬 Ghi chú: %s", attendance.getNote()));
        }

        body = bodyBuilder.toString();

        // Lấy danh sách Token của user (Học viên hoặc Phụ huynh)
        List<String> studentFcmTokens = authTokenService.getAllFcmTokensByActivePersonId(student.getPersonId());
        List<UUID> studentUserIds = userProfileService.getAllByPersonIdAndActiveTrue(student.getPersonId()).stream()
                .map(UserProfile::getUser).map(User::getUserId)
                .collect(Collectors.toList());

        Map<String, String> dataPayload = new HashMap<>();
        dataPayload.put("screen", "AttendanceHistory");
        dataPayload.put("studentId", student.getPersonId().toString());
        dataPayload.put("attendanceId", attendance.getAttendanceId().toString());
        dataPayload.put("type", "evaluation");

        notificationService.sendMulticastNotification(
                studentFcmTokens,
                studentUserIds,
                title,
                body,
                NotificationType.ATTENDANCE,
                "STUDENT_ATTENDANCE",
                attendance.getAttendanceId().toString(),
                dataPayload
        );
        log.info("Sent evaluation notification to student {} (Evaluation: {})",
                student.getFullName(), attendance.getEvaluationStatus());
    }

    /**
     * Cập nhật đánh giá (evaluation) của học viên sau buổi học.
     * <p>
     * Chức năng: Cho phép HLV đánh giá học viên sau buổi học (PASSED, FAILED, GOOD, EXCELLENT)
     * và thêm ghi chú về hiệu suất học tập.
     * <p>
     * Business Rules:
     * - Chỉ HLV có trạng thái ACTIVE mới được phép thực hiện
     * - Tự động ghi nhận HLV đã thực hiện đánh giá
     * - Có thể đánh giá sau khi đã điểm danh
     * - Ghi chú không được vượt quá 500 ký tự (validation ở DTO)
     *
     * @param coachId      Person ID của HLV thực hiện thao tác
     * @param request      Thông tin đánh giá (trạng thái đánh giá và ghi chú)
     * @param attendanceId ID của bản ghi điểm danh cần cập nhật
     * @throws NoSuchElementException nếu không tìm thấy bản ghi điểm danh
     * @throws AccessDeniedException  nếu HLV không ở trạng thái ACTIVE
     */
    @Transactional(rollbackFor = Exception.class)
//    @Caching(evict = {
//            //@CacheEvict(value = "studentDetail", allEntries = true),
//            //@CacheEvict(value = "classScheduleDetail", allEntries = true)
//    })
    public void updateAttendanceEvaluation(
            UUID coachId,
            StudentAttendanceDTO.UpdateEvaluationRequest request,
            UUID attendanceId
    ) {
        // Validate coach status
        Coach currentCoach = coachService.validateCoachAndGetActive(coachId);

        // Fetch and validate attendance record
        StudentAttendance attendance = studentAttendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Không tìm thấy bản ghi điểm danh với ID: %s", attendanceId)
                ));

        if (attendance.getAttendanceStatus() == AttendanceStatus.ABSENT
                || attendance.getAttendanceStatus() == AttendanceStatus.EXCUSED) {
            throw new IllegalStateException("Không thể đánh giá học viên vắng hoặc vắng có phép.");
        }

        // Update evaluation
        attendance.setEvaluationStatus(request.getEvaluationStatus());
        attendance.setEvaluatedByCoach(currentCoach);
        attendance.setNote(request.getNote());

        // Gửi thông báo đánh giá cho học viên (trừ trạng thái PENDING)
        if (request.getEvaluationStatus() != null &&
                request.getEvaluationStatus() != EvaluationStatus.PENDING) {
            runAfterCommit(() -> self.sendCommittedEvaluationNotification(attendance.getAttendanceId()));
        }

        publishScoreRecalculateEvent(attendance.getStudentEnrollment().getStudent(), attendance.getSessionDate());

        log.info("Coach {} updated evaluation for attendance record {} to status {}",
                currentCoach.getFullName(), attendanceId, request.getEvaluationStatus());
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void sendCommittedEvaluationNotification(UUID attendanceId) {
        StudentAttendance attendance = studentAttendanceRepository.findWithDetailsByAttendanceId(attendanceId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Không tìm thấy bản ghi điểm danh với ID: " + attendanceId
                ));

        sendEvaluationNotification(attendance);
    }

    /**
     * Lọc và lấy danh sách điểm danh cho một buổi học cụ thể.
     * <p>
     * REFACTORED: Sử dụng Specification thay cho JPQL cứng
     * <p>
     * Ưu điểm:
     * 1. Type-safe: Compiler check tại compile-time
     * 2. Dynamic: Chỉ thêm điều kiện khi tham số không null/rỗng
     * 3. Giải quyết lỗi PostgreSQL "could not determine data type"
     * 4. N+1 Query được xử lý bằng @EntityGraph trong Repository
     * 5. Clean Code: Dễ đọc, dễ maintain
     *
     * @param search                 Tìm kiếm theo tên/mã/SĐT học viên
     * @param sessionDate            Ngày học
     * @param attendanceStatuses     Danh sách trạng thái điểm danh
     * @param evaluationStatuses     Danh sách trạng thái đánh giá
     * @param belts                  Danh sách đai (belt)
     * @param branchIds              Danh sách chi nhánh
     * @param levels                 Danh sách cấp độ lớp
     * @param enrollmentHistoryItems Danh sách lịch sử đăng ký học viên (để lọc theo lịch sử enrollment)
     * @param pageable               Thông tin phân trang
     * @return PageResponse chứa danh sách StudentAttendanceDTO
     */
    @Transactional(readOnly = true)
    public StudentAttendanceDTO.AttendanceStats getStatsBySessionId(UUID sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }

        Specification<StudentAttendance> spec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("classSession").get("sessionId"), sessionId);

        return studentAttendanceRepository.getStatistics(spec);
    }

    @Transactional(readOnly = true)
    public StudentAttendanceDTO.AttendanceListResponse getStudentAttendancesWithStats(
            Pageable pageable,
            String search,
            LocalDate sessionDate,
            List<AttendanceStatus> attendanceStatuses,
            List<EvaluationStatus> evaluationStatuses,
            List<Belt> belts,
            List<Integer> branchIds,
            List<ScheduleLevel> levels,
            List<StudentEnrollmentResDTO.EnrollmentHistoryItem> enrollmentHistoryItems,
            LocalDate startDate,
            LocalDate endDate,
            List<UUID> sessionIds
    ) {
        // Chuẩn hóa tham số search (tránh trường hợp null gây lỗi)
        String safeSearch = (search == null || search.trim().isEmpty()) ? null : search.trim();

        // Build Specification động
        Specification<StudentAttendance> spec = StudentAttendanceSpecification.filterBy(
                safeSearch,
                sessionDate,
                attendanceStatuses,
                evaluationStatuses,
                belts,
                branchIds,
                levels,
                enrollmentHistoryItems,
                startDate,
                endDate
        );

        if (sessionIds != null && !sessionIds.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    root.get("classSession").get("sessionId").in(sessionIds)
            );
        }

        // Gọi Repository với Specification + Named EntityGraph (tránh N+1 query)
        // Custom method sử dụng EntityGraph được định nghĩa trong Entity
        Page<StudentAttendance> attendances = studentAttendanceRepository.findAllWithEntityGraph(spec, pageable);

        log.info("Found {} student attendances with {} total elements",
                attendances.getNumberOfElements(),
                attendances.getTotalElements());

        // Chuyển đổi sang DTO
        Page<StudentAttendanceDTO.Response> responsePage = attendances.map(studentAttendanceMapper::toResponse);

//        List<String> myAssignedScheduleIds = enrollmentHistoryItems.stream()
//                .map(StudentEnrollmentResDTO.EnrollmentHistoryItem::getScheduleId)
//                .toList();

        StudentAttendanceDTO.AttendanceStats stats = studentAttendanceRepository.getStatistics(spec);

        //4. Đóng gói vào PageResponse chuẩn
        PageResponse<StudentAttendanceDTO.Response> pageData = PageResponse.<StudentAttendanceDTO.Response>builder()
                .content(responsePage.getContent())
                .pageNumber(responsePage.getNumber())
                .pageSize(responsePage.getSize())
                .totalElements(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .first(responsePage.isFirst())
                .last(responsePage.isLast())
                .empty(responsePage.isEmpty())
                .build();

        // 5. Trả về DTO tổng chứa cả List và Stats
        return StudentAttendanceDTO.AttendanceListResponse.builder()
                .stats(stats)
                .attendances(pageData)
                .build();
    }

    // 1. HÀM CORE: Chỉ làm nhiệm vụ xử lý logic và lưu DB (Dùng cho cả Cron và API)
    // Dùng REQUIRES_NEW để nếu hàm này lỗi, nó không kéo theo Transaction của Job tổng bị rollback
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void processMissingAttendances(String scheduleId, LocalDate sessionDate) {
        ClassSession classSession = resolveUniqueClassSession(scheduleId, sessionDate);
        processMissingAttendances(classSession);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void processMissingAttendances(ClassSession classSession) {
        doProcessMissingAttendances(classSession);
    }

    @Transactional(rollbackFor = Exception.class)
    public void processMissingAttendancesInCurrentTransaction(ClassSession classSession) {
        doProcessMissingAttendances(classSession);
    }

    private void doProcessMissingAttendances(ClassSession classSession) {
        String scheduleId = classSession.getClassSchedule().getScheduleId();
        LocalDate sessionDate = classSession.getSessionDate();
        List<StudentEnrollment> activeStudents = studentEnrollmentService
                .getStudentEnrollmentsByClassScheduleId(scheduleId);

        if (activeStudents.isEmpty()) return;

        List<UUID> existingStudentIds = studentAttendanceRepository
                .findStudentIdsByClassSessionId(classSession.getSessionId());
        Set<UUID> existingStudentIdsSet = new HashSet<>(existingStudentIds);

        List<StudentAttendance> newAttendances = new ArrayList<>();
        for (StudentEnrollment enrollment : activeStudents) {
            if (!existingStudentIdsSet.contains(enrollment.getStudent().getPersonId())) {
                newAttendances.add(StudentAttendance.builder()
                        .studentEnrollment(enrollment)
                        .classSession(classSession)
                        .sessionDate(sessionDate)
                        .attendanceStatus(AttendanceStatus.ABSENT)
                        .build());
            }
        }

        if (!newAttendances.isEmpty()) {
            studentAttendanceRepository.saveAll(newAttendances);
            for (StudentAttendance sa : newAttendances) {
                publishScoreRecalculateEvent(sa.getStudentEnrollment().getStudent(), sessionDate);
            }
        }
    }

    // 2. HÀM API: Frontend gọi (Giữ nguyên tên cũ của bạn)
    @Transactional(readOnly = true) // Vì data đã được lưu ở hàm trên, hàm này chỉ cần đọc
    public List<StudentAttendanceDTO.Response> markAsAbsentByScheduleId(
            StudentAttendanceDTO.BatchCreateRequest request) {

        // Gọi core logic để init data nếu thiếu
        // Lưu ý: Để gọi hàm nội bộ mà vẫn ăn Transaction REQUIRES_NEW,
        // bạn nên inject chính Service này vào chính nó (self-invocation)
        // hoặc đưa processMissingAttendances ra một helper class.
        self.processMissingAttendances(request.getClassScheduleId(), request.getSessionDate());

        // Sau đó mới lấy Full State để trả về UI
        List<StudentAttendance> allAttendances = studentAttendanceRepository
                .findByScheduleIdAndSessionDateWithDetails(
                        request.getClassScheduleId(),
                        request.getSessionDate()
                );
        return studentAttendanceMapper.toResponseList(allAttendances);
    }

    @Transactional(rollbackFor = Exception.class)
//    @Caching(evict = {
//            //@CacheEvict(value = "studentDetail", allEntries = true),
//            //@CacheEvict(value = "classScheduleDetail", allEntries = true)
//    })
    public StudentAttendanceDTO.Response createAttendanceRecord(
            StudentAttendanceDTO.ManualLogRequest request) {
        // 1. GỘP: Validate Student, ClassSchedule và Enrollment trong 1 lần gọi
        StudentEnrollment enrollment = studentEnrollmentService
                .getEnrollmentByStudentUserIdAndClassScheduleId(request.getStudentId(), request.getClassScheduleId());

        // 2. Validate Logic nghiệp vụ (Check status ngay trên object đã fetch về)
        Student student = enrollment.getStudent();
        ClassSchedule classSchedule = enrollment.getClassSchedule();
        ClassSession classSession = resolveUniqueClassSession(
                request.getClassScheduleId(),
                request.getSessionDate()
        );

        if (student.getStudentStatus() != StudentStatus.ACTIVE) {
            throw new IllegalStateException("Học viên không ở trạng thái ACTIVE");
        }
        if (classSchedule.getScheduleStatus() != ScheduleStatus.ACTIVE) {
            throw new IllegalStateException("Lớp học đã bị hủy hoặc không hoạt động");
        }
        if (enrollment.getStatus() != StudentEnrollmentStatus.ACTIVE) {
            throw new IllegalStateException("Học viên đã nghỉ hoặc bảo lưu lớp này");
        }

        // 4. Create & Save
        StudentAttendance attendance = StudentAttendance.builder()
                .studentEnrollment(enrollment) // Đã có sẵn Student và Schedule bên trong
                .classSession(classSession)
                .sessionDate(request.getSessionDate())
                .attendanceStatus(request.getAttendanceStatus())
                .checkInTime(request.getCheckInTime() != null ? request.getCheckInTime() : LocalDateTime.now())
                .note(request.getNote())
                .build();

        StudentAttendance savedAttendance;
        try {
            savedAttendance = studentAttendanceRepository.saveAndFlush(attendance);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance already exists for this class session", e);
        }

        return studentAttendanceMapper.toResponse(savedAttendance);
    }

    private ClassSession resolveUniqueClassSession(String scheduleId, LocalDate sessionDate) {
        List<ClassSession> sessions = classSessionRepository.findBySessionDateAndClassSchedule_ScheduleId(
                sessionDate,
                scheduleId
        );
        if (sessions.size() != 1) {
            throw new IllegalStateException(String.format(
                    "Expected exactly one class session for scheduleId=%s and sessionDate=%s, found %d",
                    scheduleId,
                    sessionDate,
                    sessions.size()
            ));
        }
        return sessions.get(0);
    }

    private StudentAttendanceDTO.Response toCheckInResponse(
            StudentAttendance attendance,
            CheckInStudentProjection student,
            StudentEnrollment enrollment,
            ClassSession classSession,
            boolean alreadyCheckedIn
    ) {
        return StudentAttendanceDTO.Response.builder()
                .attendanceId(attendance.getAttendanceId())
                .enrollmentId(enrollment.getEnrollmentId())
                .studentId(student.getPersonId())
                .studentName(student.getFullName())
                .classScheduleId(classSession.getClassSchedule().getScheduleId())
                .sessionDate(attendance.getSessionDate())
                .attendanceStatus(attendance.getAttendanceStatus())
                .checkInTime(attendance.getCheckInTime())
                .alreadyCheckedIn(alreadyCheckedIn)
                .evaluationStatus(attendance.getEvaluationStatus())
                .note(attendance.getNote())
                .updatedAt(attendance.getUpdatedAt())
                .build();
    }

    @Transactional
    public void deleteAttendanceRecords(@Valid List<UUID> attendanceIds) {
        if (attendanceIds == null || attendanceIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách ID điểm danh không được để trống");
        }

        List<StudentAttendance> attendancesToDelete = studentAttendanceRepository.findAllById(attendanceIds);

        if (attendancesToDelete.size() != attendanceIds.size()) {
            throw new NoSuchElementException("Một hoặc nhiều bản ghi điểm danh không tồn tại");
        }

        studentAttendanceRepository.deleteAll(attendancesToDelete);

        // Xóa xong thì bắn event báo tính lại điểm (Lúc này hàm count = 0 -> điểm về mặc định)
        for (StudentAttendance sa : attendancesToDelete) {
            publishScoreRecalculateEvent(sa.getStudentEnrollment().getStudent(), sa.getSessionDate());
        }
    }
}
