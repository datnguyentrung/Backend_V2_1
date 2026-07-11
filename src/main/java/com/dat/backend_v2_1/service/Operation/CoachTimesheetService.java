package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.config.AttendanceProperties;
import com.dat.backend_v2_1.config.SecurityRule;
import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Operation.ClassSession;
import com.dat.backend_v2_1.domain.Operation.CoachAssignment;
import com.dat.backend_v2_1.domain.Operation.CoachTimesheet;
import com.dat.backend_v2_1.dto.Operation.CoachTimesheetDTO;
import com.dat.backend_v2_1.dto.PageResponse;
import com.dat.backend_v2_1.enums.Core.CoachStatus;
import com.dat.backend_v2_1.enums.Core.ScheduleStatus;
import com.dat.backend_v2_1.enums.Core.Weekday;
import com.dat.backend_v2_1.enums.ErrorCode;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.dat.backend_v2_1.enums.Operation.CoachTimesheetStatus;
import com.dat.backend_v2_1.enums.Operation.SessionStatus;
import com.dat.backend_v2_1.mapper.Operation.CoachTimesheetMapper;
import com.dat.backend_v2_1.repository.Core.CoachRepository;
import com.dat.backend_v2_1.repository.Operation.ClassSessionRepository;
import com.dat.backend_v2_1.repository.Operation.CoachTimesheetRepository;
import com.dat.backend_v2_1.specification.CoachTimesheetSpecification;
import com.dat.backend_v2_1.util.error.AppException;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoachTimesheetService {
    private final CoachTimesheetRepository coachTimesheetRepository;
    private final CoachAssignmentService coachAssignmentService;
    private final CoachRepository coachRepository;
    private final ClassSessionRepository classSessionRepository;
    private final CoachTimesheetMapper coachTimesheetMapper;
    private final AttendanceProperties attendanceProperties;
    private final ZoneId defaultZoneId;
    private final SecurityRule securityRule;

    @Transactional(rollbackFor = Exception.class)
    public CoachTimesheetDTO.Response scan(CoachTimesheetDTO.ScanRequest request) {
        LocalDateTime now = LocalDateTime.now(defaultZoneId);
        LocalDate today = now.toLocalDate();

        Coach coach = coachRepository.findByStaffCode(request.getStaffCode())
                .orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
        validateCoachActive(coach);

        List<CoachAssignment> activeAssignments = coachAssignmentService
                .getAllCoachAssignmentsByListCoachIds(List.of(coach.getUserId()), CoachAssignmentStatus.ACTIVE);
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
            return coachTimesheetMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.COACH_TIMESHEET_ALREADY_EXISTS);
        }
    }

    private void validateCoachActive(Coach coach) {
        if (coach.getCoachStatus() != CoachStatus.ACTIVE) {
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
        UUID ownerCoachId = timesheet.getCoachAssignment().getCoach().getUserId();
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
}
