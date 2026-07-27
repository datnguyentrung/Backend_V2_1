package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.domain.Core.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Operation.ClassSession;
import com.dat.ai_receptionist_web.dto.Operation.ClassSessionDTO;
import com.dat.ai_receptionist_web.dto.Operation.CoachTimesheetStatusProjection;
import com.dat.ai_receptionist_web.dto.Operation.ResponsibleCoachProjection;
import com.dat.ai_receptionist_web.dto.Operation.StudentAttendanceDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.enums.Core.Weekday;
import com.dat.ai_receptionist_web.enums.Operation.SessionStatus;
import com.dat.ai_receptionist_web.event.ClassSessionCompletedEvent;
import com.dat.ai_receptionist_web.mapper.Operation.ClassSessionMapper;
import com.dat.ai_receptionist_web.repository.Core.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Operation.ClassSessionRepository;
import com.dat.ai_receptionist_web.socket.ClassSessionWebSocketHandler;
import com.dat.ai_receptionist_web.specification.ClassSessionSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassSessionService {
    private final ClassSessionRepository classSessionRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ClassSessionMapper classSessionMapper;
    private final StudentAttendanceService studentAttendanceService;
    private final CoachAssignmentService coachAssignmentService;
    private final CoachTimesheetService coachTimesheetService;
    private final ClassSessionWebSocketHandler wsHandler;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    /**
     * Giữ lại biến cũ để tương thích config hiện tại.
     * Dùng như thời gian chuẩn bị/mở lớp sớm trước giờ học.
     *
     * Ví dụ:
     * - Lớp học 18:00
     * - ATTENDANCE_GRACE_PERIOD_MINUTES = 30
     * - Từ 17:30 hệ thống đã active lớp để HLV chuẩn bị điểm danh.
     */
    @Value("${ATTENDANCE_GRACE_PERIOD_MINUTES:30}")
    private int attendanceGracePeriodMinutes;

    @Value("${CLASS_SESSION_COMPLETE_AFTER_END_MINUTES}")
    private int classSessionCompleteAfterEndMinutes;

    @Scheduled(cron = "0 00 03 * * *")
    @Transactional(rollbackFor = Exception.class)
    public void generateClassSessions() {
        LocalDate today = LocalDate.now();
        Weekday currentWeekday = Weekday.valueOf(today.getDayOfWeek().name());

        log.info("Bắt đầu tiến trình sinh buổi học tự động cho ngày: {} ({})", today, currentWeekday);

        // Mọi logic so sánh đã được đẩy xuống DB, trả về đúng những lịch cần tạo
        List<ClassSchedule> schedulesNeedingSession = classScheduleRepository
                .findSchedulesNeedingSession(currentWeekday, ScheduleStatus.ACTIVE, today);

        if (schedulesNeedingSession.isEmpty()) {
            log.info("Không có lịch học nào cần sinh buổi học mới cho ngày {}.", today);
            return;
        }

        // Sinh buổi học cho mỗi lịch học phù hợp
        List<ClassSession> newSessions = schedulesNeedingSession.stream()
                .map(schedule -> ClassSession.builder()
                        .classSchedule(schedule)
                        .build())
                .toList();

        classSessionRepository.saveAll(newSessions);
        log.info("Đã sinh {} buổi học mới cho ngày {}.", newSessions.size(), today);

        broadcastAfterCommit("SESSIONS_GENERATED", Map.of("count", newSessions.size()));
    }

    /**
     * Active lớp trước giờ học.
     *
     * Chạy mỗi 5 phút, giây 00.
     * Không nên chạy cùng giây với các job khác để tránh dồn DB/WebSocket.
     */
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional(rollbackFor = Exception.class)
    public void autoActivateClassSessionsJob() {
        LocalDateTime now = LocalDateTime.now();

        /**
         * Ví dụ:
         * now = 17:30
         * attendanceGracePeriodMinutes = 30
         * thresholdTime = 18:00
         *
         * Các lớp có giờ học <= 18:00 trong ngày hôm nay sẽ được ACTIVE.
         */
        LocalTime thresholdTime = now
                .plusMinutes(attendanceGracePeriodMinutes)
                .toLocalTime();

        try {
            int updatedCount = classSessionRepository.activateScheduledSessions(
                    now.toLocalDate(),
                    thresholdTime
            );

            if (updatedCount > 0) {
                log.info(
                        "Successfully activated {} class sessions at {} with prep threshold {} minutes",
                        updatedCount,
                        now,
                        attendanceGracePeriodMinutes
                );

                broadcastAfterCommit(
                        "SESSIONS_ACTIVATED",
                        Map.of("count", updatedCount)
                );
            }
        } catch (Exception e) {
            log.error("Failed to execute autoActivateClassSessionsJob", e);
            throw e;
        }
    }

    /**
     * Auto complete session 15 minutes after endTime.
     *
     * Chạy mỗi 10 phút, giây 20.
     */
    @Scheduled(cron = "20 */10 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void autoCompleteClassSessionsJob() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thresholdDateTime = now.minusMinutes(classSessionCompleteAfterEndMinutes);
        LocalDate thresholdDate = thresholdDateTime.toLocalDate();
        LocalTime thresholdTime = thresholdDateTime.toLocalTime();

        List<ClassSession> sessionsToComplete = classSessionRepository.findClassSessionsToComplete(
                thresholdDate,
                thresholdTime
        );

        if (sessionsToComplete.isEmpty()) {
            log.debug("No class sessions require completion at {}", now);
            return;
        }

        int successCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (ClassSession candidate : sessionsToComplete) {
            UUID sessionId = candidate.getSessionId();
            String classScheduleId = candidate.getClassSchedule() == null
                    ? ""
                    : candidate.getClassSchedule().getScheduleId();

            try {
                Boolean completed = transactionTemplate.execute(status -> {
                    int updatedCount = classSessionRepository.markSessionCompleted(sessionId);
                    if (updatedCount == 0) {
                        return false;
                    }

                    broadcastAfterCommit(
                            "SESSION_COMPLETED",
                            Map.of(
                                    "sessionId", sessionId,
                                    "classScheduleId", classScheduleId
                            )
                    );
                    eventPublisher.publishEvent(new ClassSessionCompletedEvent(sessionId));

                    return true;
                });

                if (!Boolean.TRUE.equals(completed)) {
                    skippedCount++;
                    log.debug("Skipped completing session {} because it was already processed", sessionId);
                    continue;
                }

                successCount++;
                log.info(
                        "Completed class session {}. Session date: {}, end time: {}",
                        sessionId,
                        candidate.getSessionDate(),
                        candidate.getEndTime()
                );
            } catch (Exception e) {
                failedCount++;
                log.error("Failed to complete class session {}", sessionId, e);
            }
        }

        log.info(
                "Auto complete class sessions finished at {}. Completed: {}, Skipped: {}, Failed: {}, Delay after end: {} minutes",
                now,
                successCount,
                skippedCount,
                failedCount,
                classSessionCompleteAfterEndMinutes
        );
    }

    /**
     * Auto close attendance after the session passes half of its duration.
     *
     * Close time = midpoint between sessionDate + startTime and sessionDate + endTime.
     * Runs every 10 minutes, second 40.
     */
    @Scheduled(cron = "40 */10 * * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional(rollbackFor = Exception.class)
//    @Caching(evict = {
//            // Chốt tự động cũng phải xóa cache vì data thay đổi ngầm
//            //@CacheEvict(value = "studentDetail", allEntries = true),
//            //@CacheEvict(value = "classScheduleDetail", allEntries = true)
//    })
    public void autoCloseAttendanceJob() {
        LocalDateTime now = LocalDateTime.now();

        List<ClassSession> sessionsToClose = classSessionRepository
                .findClassSessionToClose(now.toLocalDate())
                .stream()
                .filter(session -> {
                    LocalDateTime attendanceCloseTime = calculateAttendanceCloseTime(session);
                    return attendanceCloseTime != null && !now.isBefore(attendanceCloseTime);
                })
                .toList();

        if (sessionsToClose.isEmpty()) {
            log.debug("No class sessions found that require attendance closure at {}", now);
            return;
        }

        int successCount = 0;
        int failedCount = 0;

        for (ClassSession session : sessionsToClose) {
            try {
                studentAttendanceService.processMissingAttendances(session);

                session.setAttendanceClosed(true);
                classSessionRepository.save(session);

                successCount++;

                log.info(
                        "Closed attendance for class session {} on date {}",
                        session.getSessionId(),
                        session.getSessionDate()
                );

                broadcastAfterCommit(
                        "SESSION_UPDATED",
                        Map.of("sessionId", session.getSessionId())
                );
            } catch (Exception e) {
                failedCount++;

                log.error(
                        "Failed to close attendance for class session {}: {}",
                        session.getSessionId(),
                        e.getMessage(),
                        e
                );
            }
        }

        log.info(
                "Auto close attendance finished at {}. Success: {}, Failed: {}",
                now,
                successCount,
                failedCount
        );
    }

    private LocalDateTime calculateAttendanceCloseTime(ClassSession session) {
        LocalDate sessionDate = session.getSessionDate();
        LocalTime startTime = session.getStartTime();
        LocalTime endTime = session.getEndTime();

        if (sessionDate == null || startTime == null || endTime == null) {
            return null;
        }

        LocalDateTime startDateTime = LocalDateTime.of(sessionDate, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(sessionDate, endTime);
        if (!endTime.isAfter(startTime)) {
            endDateTime = endDateTime.plusDays(1);
        }

        Duration sessionDuration = Duration.between(startDateTime, endDateTime);
        if (sessionDuration.isZero() || sessionDuration.isNegative()) {
            return null;
        }

        return startDateTime.plus(sessionDuration.dividedBy(2));
    }

    @Transactional(rollbackFor = Exception.class)
    public ClassSessionDTO.SessionResponse createClassSession(ClassSessionDTO.SessionCreateRequest request) {
        ClassSchedule schedule = classScheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch học với ID: " + request.getScheduleId()));

        ClassSession newSession = ClassSession.builder()
                .classSchedule(schedule)
                .sessionDate(request.getSessionDate() != null ? request.getSessionDate() : LocalDate.now())
                .status(request.getStatus())
                .isAttendanceClosed(request.isAttendanceClosed())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .note(request.getNote())
                .build();

        ClassSession savedSession = classSessionRepository.save(newSession);
        // 2. Gọi WebSocket sau khi tạo thành công
        broadcastAfterCommit("SESSION_CREATED", Map.of("sessionId", savedSession.getSessionId()));

        return classSessionMapper.toSessionResponse(savedSession);
    }

    @Transactional
    public ClassSessionDTO.SessionResponse updateClassSession(UUID sessionId,
                                                              ClassSessionDTO.SessionUpdateRequest request) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy buổi học với ID: " + sessionId));

        // MapStruct sẽ tự động map các trường khác null từ request sang session
        classSessionMapper.updateEntityFromRequest(request, session);

        // Chuyển các trường isAttendanceClosed và SessionStatus về dạng mặc định
        session.setAttendanceClosed(false);
        session.setStatus(SessionStatus.SCHEDULED);

        reconcileUpdatedSession(session, LocalDateTime.now());

        broadcastAfterCommit("SESSION_UPDATED", Map.of("sessionId", sessionId));

        return classSessionMapper.toSessionResponse(session);
    }

    /**
     * Đồng bộ trạng thái cho đúng một buổi học vừa được cập nhật.
     * Không gọi các scheduled job vì các job đó được thiết kế để quét và xử lý hàng loạt.
     */
    private void reconcileUpdatedSession(ClassSession session, LocalDateTime now) {
        LocalDateTime startDateTime = calculateSessionStartDateTime(session);
        LocalDateTime endDateTime = calculateSessionEndDateTime(session);

        if (startDateTime == null || endDateTime == null) {
            classSessionRepository.saveAndFlush(session);
            return;
        }

        LocalDateTime activationTime = startDateTime.minusMinutes(attendanceGracePeriodMinutes);
        if (!now.isBefore(activationTime)) {
            session.setStatus(SessionStatus.ACTIVE);
        }

        LocalDateTime attendanceCloseTime = calculateAttendanceCloseTime(session);
        boolean shouldCloseAttendance = session.getStatus() == SessionStatus.ACTIVE
                && attendanceCloseTime != null
                && !now.isBefore(attendanceCloseTime);
        if (shouldCloseAttendance) {
            session.setAttendanceClosed(true);
        }

        LocalDateTime completionTime =
                endDateTime.plusMinutes(classSessionCompleteAfterEndMinutes);
        boolean shouldComplete = session.getStatus() == SessionStatus.ACTIVE
                && session.isAttendanceClosed()
                && !now.isBefore(completionTime);
        if (shouldComplete) {
            session.setStatus(SessionStatus.COMPLETED);
        }

        classSessionRepository.saveAndFlush(session);

        if (shouldCloseAttendance) {
            studentAttendanceService.processMissingAttendancesInCurrentTransaction(session);
        }

        if (shouldComplete) {
            eventPublisher.publishEvent(new ClassSessionCompletedEvent(session.getSessionId()));
            broadcastAfterCommit(
                    "SESSION_COMPLETED",
                    Map.of(
                            "sessionId", session.getSessionId(),
                            "classScheduleId", session.getClassSchedule().getScheduleId()
                    )
            );
        }
    }

    private LocalDateTime calculateSessionStartDateTime(ClassSession session) {
        if (session.getSessionDate() == null || session.getStartTime() == null) {
            return null;
        }
        return LocalDateTime.of(session.getSessionDate(), session.getStartTime());
    }

    private LocalDateTime calculateSessionEndDateTime(ClassSession session) {
        LocalDateTime startDateTime = calculateSessionStartDateTime(session);
        if (startDateTime == null || session.getEndTime() == null) {
            return null;
        }

        LocalDateTime endDateTime =
                LocalDateTime.of(session.getSessionDate(), session.getEndTime());
        return session.getEndTime().isAfter(session.getStartTime())
                ? endDateTime
                : endDateTime.plusDays(1);
    }

    @Transactional
    public void deleteClassSession(UUID sessionId) {
        ClassSession existingSession = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy buổi học với ID: " + sessionId));

        classSessionRepository.delete(existingSession);

        broadcastAfterCommit("SESSION_DELETED", Map.of("sessionId", sessionId));
    }

    public ClassSessionDTO.SessionResponse getClassSessionById(UUID sessionId) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy buổi học với ID: " + sessionId));

        return classSessionMapper.toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public ClassSessionDTO.ReportData getReportData(UUID sessionId) {
        ClassSessionDTO.ReportSessionRow session = classSessionRepository.findReportSessionRow(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy buổi học: " + sessionId));

        StudentAttendanceDTO.AttendanceStats attendanceStats =
                studentAttendanceService.getStatsBySessionId(sessionId);
        List<ResponsibleCoachProjection> responsibleCoaches =
                coachAssignmentService.findResponsibleCoaches(
                        session.getClassScheduleId(),
                        session.getSessionDate()
                );

        Map<UUID, CoachTimesheetStatusProjection> timesheetsByCoachId =
                coachTimesheetService.findStatusesByClassSessionId(sessionId);

        return new ClassSessionDTO.ReportData(
                session,
                attendanceStats,
                List.copyOf(responsibleCoaches),
                Map.copyOf(timesheetsByCoachId)
        );
    }

    public PageResponse<ClassSessionDTO.SessionResponse> filterClassSessions(String search, LocalDate sessionDate,
                                                                             Boolean isAttendanceClosed,
                                                                             List<String> scheduleIds,
                                                                             Pageable pageable) {
        // Lắp ráp các điều kiện bằng phép AND
        Specification<ClassSession> spec = Specification.where(ClassSessionSpecification.hasSearch(search))
                .and(ClassSessionSpecification.hasDate(sessionDate))
                .and(ClassSessionSpecification.hasAttendanceStatus(isAttendanceClosed))
                .and(ClassSessionSpecification.hasScheduleIds(scheduleIds));

        var pageResult = classSessionRepository.findAll(spec, pageable);

        // 3. Trả về luôn! Vừa phân trang vừa map sang DTO trong 1 nốt nhạc
        return PageResponse.of(pageResult, classSessionMapper::toSessionResponse);
    }

    // Thêm hàm tiện ích này vào cuối class
    private void broadcastAfterCommit(String actionType, Map<String, Object> data) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    wsHandler.broadcastSessionChange(actionType, data);
                }
            });
        } else {
            // Nếu không nằm trong Transaction nào, thì bắn luôn
            wsHandler.broadcastSessionChange(actionType, data);
        }
    }
}
