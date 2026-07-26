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
import org.springframework.dao.DataIntegrityViolationException;
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
        Coach coach = coachTimesheet.getCoachAssignment().getCoach();
        ClassSchedule schedule = coachTimesheet.getCoachAssignment().getClassSchedule();
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

        Map<String, ClassSession> sessionByScheduleId = classSessionRepository
                .findBySessionDateAndClassSchedule_ScheduleIdIn(
                        today,
                        validAssignments.stream()
                                .map(assignment -> assignment.getClassSchedule().getScheduleId())
                                .toList()
                )
                .stream()
                .collect(Collectors.toMap(
                        session -> session.getClassSchedule().getScheduleId(),
                        Function.identity(),
                        this::preferActiveSession
                ));

        boolean hasClassToday = false;
        boolean hasWindowCandidate = false;
        boolean alreadyCheckedIn = false;

        List<ErrorCode> windowErrors = new ArrayList<>();

        for (CoachAssignment assignment : validAssignments) {
            ClassSchedule schedule = assignment.getClassSchedule();
            ClassSession classSession = sessionByScheduleId.get(schedule.getScheduleId());

            if (classSession == null) {
                continue;
            }

            hasClassToday = true;

            if (!isClassSessionUsable(classSession)) {
                continue;
            }

            Optional<ErrorCode> scanWindowError = getScanWindowError(classSession, now);

            if (scanWindowError.isPresent()) {
                windowErrors.add(scanWindowError.get());
                continue;
            }

            hasWindowCandidate = true;

            if (coachTimesheetRepository
                    .existsByCoachAssignment_AssignmentIdAndWorkingDate(
                            assignment.getAssignmentId(),
                            today
                    )) {
                alreadyCheckedIn = true;
                continue;
            }

            return createTimesheet(
                    assignment,
                    classSession,
                    now,
                    today
            );
        }

        if (alreadyCheckedIn) {
            throw new AppException(ErrorCode.COACH_TIMESHEET_ALREADY_EXISTS);
        }

        if (!hasClassToday) {
            throw new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND);
        }

        if (!hasWindowCandidate && !windowErrors.isEmpty()) {
            throw resolveWindowError(windowErrors);
        }

        throw new AppException(ErrorCode.CLASS_SESSION_NOT_FOUND);
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
                filter.getCoachAssignmentId(),
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
            CoachAssignment assignment,
            ClassSession session,
            LocalDateTime now,
            LocalDate today
    ) {
        try {
            CoachTimesheet saved = coachTimesheetRepository.saveAndFlush(CoachTimesheet.builder()
                    .coachAssignment(assignment)
                    .workingDate(today)
                    .checkInTime(now)
                    .status(CoachTimesheetStatus.CHECKED_IN)
                    .note("Coach check-in by staffCode scan")
                    .build());
            sendAttendanceNotification(saved);
            return coachTimesheetMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.COACH_TIMESHEET_ALREADY_EXISTS);
        }
    }

    private void validateCoachActive(CoachStatus coachStatus) {
        if (coachStatus != CoachStatus.ACTIVE) {
            throw new AppException(ErrorCode.COACH_INACTIVE);
        }
    }

    private boolean isClassSessionUsable(ClassSession classSession) {
        if (classSession.getStatus() == SessionStatus.CANCELLED || classSession.getStatus() == SessionStatus.TERMINATED) {
            return false;
        }
        return !classSession.isAttendanceClosed();
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

    private ClassSession preferActiveSession(ClassSession left, ClassSession right) {
        if (left.getStatus() == SessionStatus.ACTIVE) {
            return left;
        }
        if (right.getStatus() == SessionStatus.ACTIVE) {
            return right;
        }
        return left;
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
        UUID ownerCoachId = timesheet.getCoachAssignment().getCoach().getPersonId();
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
    public Map<UUID, CoachTimesheetStatusProjection> findStatusesByAssignmentIds(
            Collection<UUID> assignmentIds,
            LocalDate workingDate
    ) {
        if (assignmentIds == null || assignmentIds.isEmpty() || workingDate == null) {
            return Map.of();
        }

        return coachTimesheetRepository
                .findStatusByAssignmentIdsAndWorkingDate(assignmentIds, workingDate)
                .stream()
                .filter(row -> row.getAssignmentId() != null)
                .collect(Collectors.toMap(
                        CoachTimesheetStatusProjection::getAssignmentId,
                        Function.identity(),
                        (first, second) -> first
                ));
    }

    public String buildResponsibleCoachReportSummary(
            List<ResponsibleCoachProjection> assignments,
        Map<UUID, CoachTimesheetStatusProjection> timesheetByAssignmentId
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
                    CoachTimesheetStatusProjection timesheet = timesheetByAssignmentId.get(assignment.getAssignmentId());
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
