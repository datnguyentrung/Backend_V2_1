package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.config.AttendanceProperties;
import com.dat.ai_receptionist_web.config.SecurityRule;
import com.dat.ai_receptionist_web.domain.Core.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Core.Coach;
import com.dat.ai_receptionist_web.domain.Operation.ClassSession;
import com.dat.ai_receptionist_web.domain.Operation.CoachAssignment;
import com.dat.ai_receptionist_web.domain.Operation.CoachTimesheet;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Operation.CoachAssignmentResDTO;
import com.dat.ai_receptionist_web.dto.Operation.CoachTimesheetStatusProjection;
import com.dat.ai_receptionist_web.dto.Operation.CoachTimesheetDTO;
import com.dat.ai_receptionist_web.dto.Operation.ResponsibleCoachProjection;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Core.CoachStatus;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.enums.Core.Weekday;
import com.dat.ai_receptionist_web.enums.ErrorCode;
import com.dat.ai_receptionist_web.enums.Operation.CoachAssignmentStatus;
import com.dat.ai_receptionist_web.enums.Operation.CoachTimesheetStatus;
import com.dat.ai_receptionist_web.enums.Operation.NotificationType;
import com.dat.ai_receptionist_web.enums.Operation.SessionStatus;
import com.dat.ai_receptionist_web.mapper.Operation.CoachTimesheetMapper;
import com.dat.ai_receptionist_web.repository.Core.CoachRepository;
import com.dat.ai_receptionist_web.repository.Operation.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Operation.CoachTimesheetRepository;
import com.dat.ai_receptionist_web.service.Security.AuthTokenService;
import com.dat.ai_receptionist_web.service.Security.UserService;
import com.dat.ai_receptionist_web.specification.CoachTimesheetSpecification;
import com.dat.ai_receptionist_web.util.error.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoachTimesheetService {
    private static final String HEAD_COACH_ROLE_CODE = "HEAD_COACH";

    private final CoachTimesheetRepository coachTimesheetRepository;
    private final CoachAssignmentService coachAssignmentService;
    private final CoachRepository coachRepository;
    private final ClassSessionRepository classSessionRepository;
    private final CoachTimesheetMapper coachTimesheetMapper;
    private final AttendanceProperties attendanceProperties;
    private final ZoneId defaultZoneId;
    private final SecurityRule securityRule;
    private final NotificationService notificationService;
    private final AuthTokenService authTokenService;
    private final UserService userService;

    private void sendAttendanceNotification(CoachTimesheet coachTimesheet) {
        Coach coach = coachTimesheet.getCoach();
        ClassSchedule schedule = coachTimesheet.getClassSession().getClassSchedule();
        String scheduleId = schedule.getScheduleId();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
        DateTimeFormatter sessionTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("vi-VN"));
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
        String formattedTime = coachTimesheet.getCheckInTime() != null
                ? coachTimesheet.getCheckInTime().atZone(defaultZoneId).format(timeFormatter)
                : coachTimesheet.getCreatedAt().atZone(defaultZoneId).format(timeFormatter);
        String coachDisplayName = "HLV " + coach.getFullName();
        String sessionStartTime = schedule.getStartTime().format(sessionTimeFormatter);
        String workingDate = coachTimesheet.getWorkingDate().format(dateFormatter);

        String title;
        String body;

        switch (coachTimesheet.getStatus()) {
            case CHECKED_IN:
                title = "Điểm danh HLV thành công";
                body = String.format("%s đã điểm danh thành công cho lớp %s tại cơ sở %s, ca %s.\nGiờ bắt đầu buổi học: %s.\nThời điểm điểm danh: %s.",
                        coachDisplayName,
                        scheduleId,
                        scheduleId.charAt(1),
                        scheduleId.charAt(4),
                        sessionStartTime,
                        formattedTime);
                break;
            case ADJUSTED:
                title = "Cập nhật chấm công HLV";
                body = String.format("Bảng công của %s cho lớp %s ngày %s đã được điều chỉnh.\nGiờ bắt đầu buổi học: %s.\nThời gian ghi nhận: %s.",
                        coachDisplayName,
                        scheduleId,
                        workingDate,
                        sessionStartTime,
                        formattedTime);
                break;
            case APPROVED:
                title = "Bảng công HLV đã được duyệt";
                body = String.format("Bảng công của %s cho lớp %s ngày %s đã được duyệt.\nGiờ bắt đầu buổi học: %s.\nThời điểm điểm danh: %s.",
                        coachDisplayName,
                        scheduleId,
                        workingDate,
                        sessionStartTime,
                        formattedTime);
                break;
            case REJECTED:
                title = "Bảng công HLV bị từ chối";
                body = String.format("Bảng công của %s cho lớp %s ngày %s đã bị từ chối.\nGiờ bắt đầu buổi học: %s.\nThời điểm điểm danh: %s.",
                        coachDisplayName,
                        scheduleId,
                        workingDate,
                        sessionStartTime,
                        formattedTime);
                break;
            case CANCELLED:
                return;
            case PENDING:
            default:
                title = "Thông báo chấm công HLV";
                body = String.format("Bảng công của %s cho lớp %s ngày %s đã được cập nhật trạng thái: %s.\nGiờ bắt đầu buổi học: %s.\nThời gian ghi nhận: %s.",
                        coachDisplayName,
                        scheduleId,
                        workingDate,
                        coachTimesheet.getStatus(),
                        sessionStartTime,
                        formattedTime);
        }

        List<String> notificationTokens = new ArrayList<>();
        notificationTokens.addAll(authTokenService.getAllFcmTokensByActivePersonId(coach.getPersonId()));
        notificationTokens.addAll(authTokenService.getAllFcmTokensByRoleCode(HEAD_COACH_ROLE_CODE));
        notificationTokens = notificationTokens.stream()
                .filter(token -> token != null && !token.isEmpty())
                .distinct()
                .toList();

        List<UUID> recipientUserIds = new ArrayList<>();
        recipientUserIds.add(coach.getPersonId());
        recipientUserIds.addAll(userService.getAllUsersByRoleCode(HEAD_COACH_ROLE_CODE).stream()
                .map(User::getUserId)
                .toList());
        recipientUserIds = recipientUserIds.stream().distinct().toList();

        Map<String, String> dataPayload = new HashMap<>();
        dataPayload.put("screen", "CoachTimesheet");
        dataPayload.put("coachId", coach.getPersonId().toString());
        dataPayload.put("timesheetId", coachTimesheet.getTimesheetId().toString());

        notificationService.sendMulticastNotification(
                notificationTokens,
                recipientUserIds,
                title,
                body,
                NotificationType.COACH_TIMESHEET,
                "COACH_TIMESHEET",
                coachTimesheet.getTimesheetId().toString(),
                dataPayload
        );

    }

    @Transactional(rollbackFor = Exception.class)
    public CoachTimesheetDTO.Response checkIn(CoachTimesheetDTO.CheckInRequest request) {
        Coach coach = request.getStaffCode() != null ?
                coachRepository.findByStaffCode(request.getStaffCode())
                .orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND)) :
                coachRepository.findById(request.getPersonId())
                .orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
        return checkInResolvedCoach(coach.getPersonId(), coach.getCoachStatus());
    }

    /**
     * Internal fast path for face check-in. The caller has already resolved the coach
     * while identifying the check-in target, so no second coach lookup is needed.
     */
    @Transactional(rollbackFor = Exception.class)
    public CoachTimesheetDTO.Response checkInResolvedCoach(UUID coachId, CoachStatus coachStatus) {
        LocalDateTime now = LocalDateTime.now(defaultZoneId);
        LocalDate today = now.toLocalDate();
        validateCoachActive(coachStatus);
        Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        List<CoachAssignment> validAssignments = getValidAssignments(coachId, today);
        List<ClassSession> sessions = classSessionRepository
                .findBySessionDateAndClassSchedule_ScheduleIdIn(
                        today,
                        validAssignments.stream().map(a -> a.getClassSchedule().getScheduleId()).distinct().toList()
                );
        ClassSession classSession = selectAutomaticSession(sessions);
        getScanWindowError(classSession, now).ifPresent(error -> { throw new AppException(error); });

        boolean hasAssignmentForSession = validAssignments.stream()
                .filter(item -> item.getClassSchedule().getScheduleId()
                        .equals(classSession.getClassSchedule().getScheduleId()))
                .findAny()
                .isPresent();
        if (!hasAssignmentForSession) {
            throw new AppException(ErrorCode.COACH_ASSIGNMENT_INVALID);
        }
        return checkInForSession(coach, classSession, now, today);
    }

    private List<CoachAssignment> getValidAssignments(UUID coachId, LocalDate today) {
        List<CoachAssignment> activeAssignments = coachAssignmentService
                .getAllCoachAssignmentsByListCoachIds(List.of(coachId), CoachAssignmentStatus.ACTIVE);
        if (activeAssignments.isEmpty()) {
            throw new AppException(ErrorCode.COACH_ASSIGNMENT_INVALID);
        }

        List<CoachAssignment> validAssignments = activeAssignments.stream()
                .filter(assignment -> assignment.isEffectiveOn(today))
                .filter(assignment -> assignment.getClassSchedule() != null)
                .filter(assignment -> assignment.getClassSchedule().getScheduleStatus() == ScheduleStatus.ACTIVE)
                .filter(assignment -> assignment.getClassSchedule().getWeekday() == Weekday.fromJavaDayOfWeek(today.getDayOfWeek()))
                .sorted(Comparator.comparing(assignment -> assignment.getClassSchedule().getStartTime()))
                .toList();
        if (validAssignments.isEmpty()) {
            throw new AppException(ErrorCode.COACH_ASSIGNMENT_INVALID);
        }

        return validAssignments;
    }

    private CoachTimesheetDTO.Response checkInForSession(
            Coach coach, ClassSession classSession, LocalDateTime now, LocalDate today) {
        Optional<CoachTimesheet> existingTimesheet = coachTimesheetRepository
                .findForCheckInByCoach_PersonIdAndClassSession_SessionId(coach.getPersonId(), classSession.getSessionId());
        if (existingTimesheet.isPresent()) {
            CoachTimesheet timesheet = existingTimesheet.get();
            if (timesheet.getCheckInTime() != null) {
                return toCheckInResponse(timesheet, true);
            }
            timesheet.setCheckInTime(now);
            timesheet.setStatus(CoachTimesheetStatus.CHECKED_IN);
            CoachTimesheet saved = coachTimesheetRepository.saveAndFlush(timesheet);
            sendAttendanceNotification(saved);
            return toCheckInResponse(saved, false);
        }
        return createTimesheet(coach, classSession, now, today);
    }

    @Transactional(readOnly = true)
    public CoachTimesheetDTO.Response getDetail(UUID timesheetId, Authentication authentication) {
        CoachTimesheet timesheet = coachTimesheetRepository.findWithDetailsByTimesheetId(timesheetId)
                .orElseThrow(() -> new AppException(ErrorCode.COACH_TIMESHEET_NOT_FOUND));
        assertCanView(timesheet, authentication);
        return coachTimesheetMapper.toResponse(timesheet);
    }

    @Transactional(readOnly = true)
    public CoachTimesheetDTO.TimesheetListResponse filter(
            CoachTimesheetDTO.FilterRequest filter,
            Pageable pageable,
            Authentication authentication
    ) {
        validateDateRange(filter.getFromDate(), filter.getToDate());

        if (!isManager(authentication)) {
            UUID currentCoachId = currentUserId(authentication);
            if (filter.getCoachId() != null && !filter.getCoachId().equals(currentCoachId)) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            filter.setCoachId(currentCoachId);
        }

        Specification<CoachTimesheet> spec = CoachTimesheetSpecification.filterBy(
                filter.getCoachId(),
                filter.getClassSessionId(),
                filter.getClassScheduleId(),
                filter.getBranchId(),
                filter.getStatus(),
                filter.getWorkDate(),
                filter.getFromDate(),
                filter.getToDate(),
                filter.getYearMonth(),
                filter.getSearch()
        );

        Page<CoachTimesheet> page = coachTimesheetRepository.findAllWithEntityGraph(spec, pageable);
        PageResponse<CoachTimesheetDTO.Response> pageResponse = PageResponse.of(page, coachTimesheetMapper::toResponse);
        CoachTimesheetDTO.SummaryResponse summary = coachTimesheetRepository.getSummary(spec);

        return CoachTimesheetDTO.TimesheetListResponse.builder()
                .summary(summary)
                .timesheets(pageResponse)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public CoachTimesheetDTO.Response adjust(UUID timesheetId, CoachTimesheetDTO.AdjustRequest request) {
        CoachTimesheet timesheet = coachTimesheetRepository.findWithDetailsByTimesheetId(timesheetId)
                .orElseThrow(() -> new AppException(ErrorCode.COACH_TIMESHEET_NOT_FOUND));

        if (request.getStatus() != null) timesheet.setStatus(request.getStatus());
        if (request.getCheckInTime() != null) timesheet.setCheckInTime(request.getCheckInTime());
        if (request.getCheckOutTime() != null) timesheet.setCheckOutTime(request.getCheckOutTime());
        if (request.getNote() != null) timesheet.setNote(request.getNote());
        if (request.getStatus() == null) timesheet.setStatus(CoachTimesheetStatus.ADJUSTED);

        return coachTimesheetMapper.toResponse(timesheet);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(UUID timesheetId) {
        int deletedRows = coachTimesheetRepository.deleteByTimesheetId(timesheetId);
        if (deletedRows == 0) {
            throw new AppException(ErrorCode.COACH_TIMESHEET_NOT_FOUND);
        }
    }

    private CoachTimesheetDTO.Response createTimesheet(
            Coach coach,
            ClassSession session,
            LocalDateTime now,
            LocalDate today
    ) {
        CoachTimesheet saved = coachTimesheetRepository.saveAndFlush(CoachTimesheet.builder()
                .coach(coach)
                .classSession(session)
                .workingDate(today)
                .checkInTime(now)
                .status(CoachTimesheetStatus.CHECKED_IN)
                .note("Coach check-in by staffCode scan")
                .build());
        sendAttendanceNotification(saved);
        log.info("CHECK_IN_RESULT personId={} timesheetId={} alreadyCheckedIn=false previousStatus=null newStatus={}",
                coach.getPersonId(), saved.getTimesheetId(), CoachTimesheetStatus.CHECKED_IN);
        return toCheckInResponse(saved, false);
    }

    private CoachTimesheetDTO.Response toCheckInResponse(CoachTimesheet timesheet, boolean alreadyCheckedIn) {
        CoachTimesheetDTO.Response response = coachTimesheetMapper.toResponse(timesheet);
        response.setAttendanceId(timesheet.getTimesheetId());
        response.setAlreadyCheckedIn(alreadyCheckedIn);
        return response;
    }

    private void validateCoachActive(CoachStatus coachStatus) {
        if (coachStatus != CoachStatus.ACTIVE) {
            throw new AppException(ErrorCode.COACH_INACTIVE);
        }
    }

    private boolean isClassSessionUsable(ClassSession classSession) {
        return classSession.getStatus() == SessionStatus.ACTIVE
                && !classSession.isAttendanceClosed();
    }

    private Optional<ErrorCode> getScanWindowError(ClassSession classSession, LocalDateTime now) {
        LocalDateTime start = LocalDateTime.of(now.toLocalDate(), classSession.getStartTime());
        LocalDateTime earliest = start.minusMinutes(attendanceProperties.getEarlyCheckInMinutes());
        LocalDateTime latest = start.plusMinutes(attendanceProperties.getLateCheckInLimitMinutes());

        if (now.isBefore(earliest)) {
            return Optional.of(ErrorCode.CHECK_IN_TOO_EARLY);
        }
        if (now.isAfter(latest)) {
            return Optional.of(ErrorCode.CHECK_IN_TOO_LATE);
        }
        return Optional.empty();
    }

    private AppException resolveWindowError(List<ErrorCode> windowErrors) {
        boolean hasUpcomingSession = windowErrors.stream()
                .anyMatch(error -> error == ErrorCode.CHECK_IN_TOO_EARLY);

        return new AppException(
                hasUpcomingSession
                        ? ErrorCode.CHECK_IN_TOO_EARLY
                        : ErrorCode.CHECK_IN_TOO_LATE
        );
    }

    private ClassSession selectAutomaticSession(List<ClassSession> sessions) {
        List<ClassSession> ordered = sessions.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ClassSession::getStartTime)
                        .thenComparing(ClassSession::getSessionId))
                .toList();

        ClassSession selected = ordered.stream()
                .filter(this::isClassSessionUsable)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND));

        boolean hasEarlierUnfinishedSession = ordered.stream()
                .takeWhile(session -> !session.getSessionId().equals(selected.getSessionId()))
                .anyMatch(session -> !isTerminal(session));
        if (hasEarlierUnfinishedSession) {
            throw new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND);
        }

        long sameStartTimeCandidates = ordered.stream()
                .filter(this::isClassSessionUsable)
                .filter(session -> session.getStartTime().equals(selected.getStartTime()))
                .count();
        if (sameStartTimeCandidates > 1) {
            throw new AppException(ErrorCode.MULTIPLE_ACTIVE_CLASS_SESSIONS);
        }
        return selected;
    }

    private boolean isTerminal(ClassSession session) {
        return session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.CANCELLED
                || session.getStatus() == SessionStatus.TERMINATED;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void assertCanView(CoachTimesheet timesheet, Authentication authentication) {
        if (isManager(authentication)) {
            return;
        }
        UUID currentCoachId = currentUserId(authentication);
        UUID ownerCoachId = timesheet.getCoach().getPersonId();
        if (!currentCoachId.equals(ownerCoachId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private boolean isManager(Authentication authentication) {
        return securityRule.isManagerSenior(authentication) || securityRule.isHeadCoach(authentication);
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return UUID.fromString(authentication.getName());
    }

    public String buildResponsibleCoachSummary(
            List<CoachAssignmentResDTO.Response> assignments,
            Map<UUID, CoachTimesheet> timesheetByAssignmentId
    ) {
        if (assignments == null || assignments.isEmpty()) {
            return "chưa xác định được HLV phụ trách.";
        }

        DateTimeFormatter timeFormatter =
                DateTimeFormatter.ofPattern(
                        "HH:mm",
                        Locale.forLanguageTag("vi-VN")
                );

        return assignments.stream()
                .filter(Objects::nonNull)
                .map(assignment -> {
                    String coachName = "Không xác định";

                    if (assignment.getCoach() != null &&
                            assignment.getCoach().getFullName() != null &&
                            !assignment.getCoach().getFullName().isBlank()) {

                        coachName = assignment.getCoach()
                                .getFullName()
                                .trim();
                    }

                    CoachTimesheet timesheet =
                            timesheetByAssignmentId.get(
                                    assignment.getAssignmentId()
                            );

                    if (timesheet == null) {
                        return String.format(
                                "HLV %s — chưa chấm công",
                                coachName
                        );
                    }

                    String statusText =
                            CoachTimesheetStatus.getCoachTimesheetStatusText(
                                    timesheet.getStatus()
                            );

                    String checkInText = "";

                    if (timesheet.getCheckInTime() != null) {
                        checkInText = " lúc " +
                                timesheet.getCheckInTime()
                                        .format(timeFormatter);
                    }

                    return String.format(
                            "HLV %s — %s%s",
                            coachName,
                            statusText,
                            checkInText
                    );
                })
                .distinct()
                .collect(Collectors.joining("; "));
    }

    @Transactional(readOnly = true)
    public Map<UUID, CoachTimesheetStatusProjection> findStatusesByClassSessionId(UUID classSessionId) {
        if (classSessionId == null) {
            return Map.of();
        }

        return coachTimesheetRepository
                .findStatusByClassSessionId(classSessionId)
                .stream()
                .filter(row -> row.getCoachId() != null)
                .collect(Collectors.toMap(
                        CoachTimesheetStatusProjection::getCoachId,
                        Function.identity(),
                        (first, second) -> first
                ));
    }

    public String buildResponsibleCoachReportSummary(
            List<ResponsibleCoachProjection> assignments,
        Map<UUID, CoachTimesheetStatusProjection> timesheetByCoachId
    ) {
        if (assignments == null || assignments.isEmpty()) {
            return "chưa xác định được HLV phụ trách.";
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("vi-VN"));

        return assignments.stream()
                .filter(Objects::nonNull)
                .map(assignment -> {
                    String coachName = assignment.getCoachName() == null || assignment.getCoachName().isBlank()
                            ? "Không xác định"
                            : assignment.getCoachName().trim();
                    CoachTimesheetStatusProjection timesheet = timesheetByCoachId.get(assignment.getCoachPersonId());
                    if (timesheet == null) {
                        return String.format("HLV %s - chưa chấm công", coachName);
                    }

                    String statusText = CoachTimesheetStatus.getCoachTimesheetStatusText(timesheet.getStatus());
                    String checkInText = timesheet.getCheckInTime() == null
                            ? ""
                            : " lúc " + timesheet.getCheckInTime().format(timeFormatter);
                    return String.format("HLV %s - %s%s", coachName, statusText, checkInText);
                })
                .distinct()
                .collect(Collectors.joining("; "));
    }
}
