package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Operation.CoachAssignment;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentReqDTO;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.dto.Operation.ResponsibleCoachProjection;
import com.dat.backend_v2_1.dto.PageResponse;
import com.dat.backend_v2_1.enums.Core.CoachStatus;
import com.dat.backend_v2_1.enums.Core.ScheduleStatus;
import com.dat.backend_v2_1.enums.ErrorCode;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.dat.backend_v2_1.mapper.Operation.CoachAssignmentMapper;
import com.dat.backend_v2_1.repository.Core.CoachRepository;
import com.dat.backend_v2_1.repository.Operation.CoachAssignmentRepository;
import com.dat.backend_v2_1.service.Core.ClassScheduleService;
import com.dat.backend_v2_1.specification.CoachAssignmentSpecification;
import com.dat.backend_v2_1.util.error.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoachAssignmentService {
    private final CoachAssignmentRepository coachAssignmentRepository;
    private final CoachRepository coachRepository;
    private final ClassScheduleService classScheduleService;
    private final CoachAssignmentMapper coachAssignmentMapper;

    @Transactional(readOnly = true)
    public List<ResponsibleCoachProjection> findResponsibleCoaches(String scheduleId, LocalDate sessionDate) {
        if (scheduleId == null || scheduleId.isBlank() || sessionDate == null) {
            return List.of();
        }

        return coachAssignmentRepository.findResponsibleCoaches(
                scheduleId.trim(),
                sessionDate,
                CoachAssignmentStatus.ACTIVE
        );
    }

    @Transactional(readOnly = true)
    public List<CoachAssignment> getAllCoachAssignmentsByListCoachIds(List<UUID> coachIds, CoachAssignmentStatus status) {
        return coachAssignmentRepository.findByCoachIdInAndStatusWithClassSchedule(coachIds, status);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "coachAssignments", allEntries = true),
            @CacheEvict(value = "detailedCoachAssignments", allEntries = true),
            @CacheEvict(value = "coachDetail", allEntries = true),
            @CacheEvict(value = "classScheduleDetail", allEntries = true),
            @CacheEvict(value = "classScheduleList", allEntries = true)
    })
    public List<CoachAssignment> createCoachAssignment(CoachAssignmentReqDTO.CreateRequest request) {
        Coach coach = coachRepository.findByStaffCode(request.getCoachId())
                .orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
        validateCoachActive(coach);

        List<ClassSchedule> schedules = classScheduleService.findByScheduleIds(request.getScheduleIds());
        if (schedules.size() != request.getScheduleIds().size()) {
            throw new AppException(ErrorCode.CLASS_NOT_FOUND);
        }

        LocalDate effectiveEnd = request.getEndDate() == null ? LocalDate.of(9999, 12, 31) : request.getEndDate();
        List<CoachAssignmentStatus> blockingStatuses = List.of(
                CoachAssignmentStatus.ACTIVE,
                CoachAssignmentStatus.PENDING,
                CoachAssignmentStatus.SUSPENDED
        );

        List<CoachAssignment> assignments = schedules.stream().map(schedule -> {
            validateClassActive(schedule);
            validateNoOverlap(coach.getPersonId(), schedule, request.getAssignmentDate(), effectiveEnd, blockingStatuses, null);

            CoachAssignment entity = coachAssignmentMapper.toEntity(request);
            entity.setCoach(coach);
            entity.setClassSchedule(schedule);
            entity.setStatus(CoachAssignmentStatus.ACTIVE);
            return entity;
        }).toList();

        List<CoachAssignment> saved = coachAssignmentRepository.saveAll(assignments);
        log.info("Assigned coach {} to {} classes", coach.getPersonId(), saved.size());
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "coachAssignments", allEntries = true),
            @CacheEvict(value = "detailedCoachAssignments", allEntries = true),
            @CacheEvict(value = "coachDetail", allEntries = true),
            @CacheEvict(value = "classScheduleDetail", allEntries = true),
            @CacheEvict(value = "classScheduleList", allEntries = true)
    })
    public void deleteCoachAssignment(UUID coachAssignmentId) {
        CoachAssignment assignment = coachAssignmentRepository.findById(coachAssignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.COACH_ASSIGNMENT_NOT_FOUND));
        assignment.setStatus(CoachAssignmentStatus.CANCELLED);
        assignment.setEndDate(LocalDate.now());
        log.info("Cancelled CoachAssignment with id {}", coachAssignmentId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "coachAssignments", allEntries = true),
            @CacheEvict(value = "detailedCoachAssignments", allEntries = true),
            @CacheEvict(value = "coachDetail", allEntries = true),
            @CacheEvict(value = "classScheduleDetail", allEntries = true),
            @CacheEvict(value = "classScheduleList", allEntries = true)
    })
    public void updateCoachAssignment(UUID coachAssignmentId, CoachAssignmentReqDTO.UpdateRequest request) {
        CoachAssignment assignment = coachAssignmentRepository.findWithDetailsByAssignmentId(coachAssignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.COACH_ASSIGNMENT_NOT_FOUND));

        LocalDate start = request.getAssignmentDate() == null ? assignment.getAssignedDate() : request.getAssignmentDate();
        LocalDate end = request.getEndDate() == null ? assignment.getEndDate() : request.getEndDate();
        LocalDate effectiveEnd = end == null ? LocalDate.of(9999, 12, 31) : end;
        CoachAssignmentStatus targetStatus = request.getStatus() == null ? assignment.getStatus() : request.getStatus();

        if (targetStatus.blocksNewAssignment()) {
            validateNoOverlap(
                    assignment.getCoach().getPersonId(),
                    assignment.getClassSchedule(),
                    start,
                    effectiveEnd,
                    List.of(CoachAssignmentStatus.ACTIVE, CoachAssignmentStatus.PENDING, CoachAssignmentStatus.SUSPENDED),
                    coachAssignmentId
            );
        }

        coachAssignmentMapper.updateEntityFromDto(request, assignment);
        log.info("Updated CoachAssignment with id {}", coachAssignmentId);
    }

    @Transactional(readOnly = true)
    public CoachAssignmentResDTO.Response getCoachAssignmentDetail(UUID assignmentId) {
        return coachAssignmentRepository.findWithDetailsByAssignmentId(assignmentId)
                .map(coachAssignmentMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.COACH_ASSIGNMENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public PageResponse<CoachAssignmentResDTO.Response> filterCoachAssignments(
            UUID coachId,
            String classScheduleId,
            Integer branchId,
            CoachAssignmentStatus status,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate effectiveDate,
            String search,
            Pageable pageable
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
        Specification<CoachAssignment> spec = CoachAssignmentSpecification.filterBy(
                coachId, classScheduleId, branchId, status, startDate, endDate, effectiveDate, search
        );
        Page<CoachAssignment> page = coachAssignmentRepository.findAll(spec, pageable);
        return PageResponse.of(page, coachAssignmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public boolean existsValidAssignment(UUID coachId, String scheduleId, LocalDate workDate) {
        return coachAssignmentRepository.findValidAssignment(
                coachId,
                scheduleId,
                workDate,
                CoachAssignmentStatus.ACTIVE
        ).isPresent();
    }

    @Transactional(readOnly = true)
    public CoachAssignment getValidAssignment(UUID coachId, String scheduleId, LocalDate workDate) {
        CoachAssignment assignment = coachAssignmentRepository.findValidAssignment(
                coachId,
                scheduleId,
                workDate,
                CoachAssignmentStatus.ACTIVE
        ).orElseThrow(() -> new AppException(ErrorCode.COACH_ASSIGNMENT_INVALID));

        if (assignment.getAssignedDate() != null && workDate.isBefore(assignment.getAssignedDate())) {
            throw new AppException(ErrorCode.COACH_ASSIGNMENT_NOT_STARTED);
        }
        if (assignment.getEndDate() != null && workDate.isAfter(assignment.getEndDate())) {
            throw new AppException(ErrorCode.COACH_ASSIGNMENT_ENDED);
        }
        return assignment;
    }

    @Cacheable(value = "coachAssignments", key = "#coachId.toString() + '_' + #status.name()", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<CoachAssignmentResDTO.SimpleResponse> findCoachAssignmentsByCoachId(UUID coachId, CoachAssignmentStatus status) {
        coachRepository.findById(coachId).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
        return coachAssignmentRepository.findByCoach_PersonIdAndStatusWithClassSchedule(coachId, status)
                .stream()
                .map(coachAssignmentMapper::toSimpleResponse)
                .toList();
    }

    @Cacheable(value = "detailedCoachAssignments", key = "#coachId.toString() + '_' + #status.name()", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<CoachAssignmentResDTO.Response> findDetailedCoachAssignmentsByCoachId(UUID coachId, CoachAssignmentStatus status) {
        coachRepository.findById(coachId).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
        return coachAssignmentRepository.findByCoach_PersonIdAndStatusWithClassSchedule(coachId, status)
                .stream()
                .map(coachAssignmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CoachAssignmentResDTO.SimpleResponse> getAllCoachAssignmentsByStatus(CoachAssignmentStatus status) {
        return coachAssignmentRepository.findByStatus(status)
                .stream()
                .map(coachAssignmentMapper::toSimpleResponse)
                .toList();
    }

    private void validateCoachActive(Coach coach) {
        if (coach.getCoachStatus() != CoachStatus.ACTIVE) {
            throw new AppException(ErrorCode.COACH_INACTIVE);
        }
    }

    private void validateClassActive(ClassSchedule schedule) {
        if (schedule.getScheduleStatus() != ScheduleStatus.ACTIVE) {
            throw new AppException(ErrorCode.CLASS_INACTIVE);
        }
    }

    private void validateNoOverlap(
            UUID coachId,
            ClassSchedule schedule,
            LocalDate start,
            LocalDate effectiveEnd,
            List<CoachAssignmentStatus> blockingStatuses,
            UUID excludedId
    ) {
        boolean overlapped = coachAssignmentRepository.existsOverlappingAssignment(
                coachId,
                schedule.getWeekday(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                start,
                effectiveEnd,
                blockingStatuses,
                excludedId
        );
        if (overlapped) {
            throw new AppException(ErrorCode.COACH_ASSIGNMENT_OVERLAPPED);
        }
    }
}
