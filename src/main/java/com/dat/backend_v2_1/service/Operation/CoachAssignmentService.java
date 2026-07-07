package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Operation.CoachAssignment;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentReqDTO;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.enums.ErrorCode;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.dat.backend_v2_1.mapper.Operation.CoachAssignmentMapper;
import com.dat.backend_v2_1.repository.Core.CoachRepository;
import com.dat.backend_v2_1.repository.Operation.CoachAssignmentRepository;
import com.dat.backend_v2_1.service.Core.ClassScheduleService;
import com.dat.backend_v2_1.util.error.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoachAssignmentService {
    private final CoachAssignmentRepository coachAssignmentRepository;
    private final CoachRepository coachRepository;
    private final ClassScheduleService classScheduleService;
    private final CoachAssignmentMapper coachAssignmentMapper;

    /**
     * Internal logic, trả Entity nên không cache.
     */
    @Transactional(readOnly = true)
    public List<CoachAssignment> getAllCoachAssignmentsByListCoachIds(
            List<UUID> coachIds,
            CoachAssignmentStatus status
    ) {
        return coachAssignmentRepository.findByCoachIdInAndStatusWithClassSchedule(coachIds, status);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "coachAssignments", allEntries = true),
            @CacheEvict(value = "detailedCoachAssignments", allEntries = true),
            @CacheEvict(value = "coachDetail", allEntries = true),

            // Quan trọng: phân công HLV thay đổi coaches trong ClassScheduleDetail/List
            @CacheEvict(value = "classScheduleDetail", allEntries = true),
            @CacheEvict(value = "classScheduleList", allEntries = true)
    })
    public List<CoachAssignment> createCoachAssignment(CoachAssignmentReqDTO.CreateRequest request) {
        Coach coach = coachRepository.findById(UUID.fromString(request.getCoachId()))
                .orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        List<ClassSchedule> schedules = classScheduleService.findByScheduleIds(request.getScheduleIds());

        if (schedules.size() != request.getScheduleIds().size()) {
            throw new IllegalArgumentException("One or more ClassSchedule IDs are invalid");
        }

        List<CoachAssignment> existingAssignments = coachAssignmentRepository
                .findByCoachAndScheduleIdsAndStatus(
                        coach.getUserId(),
                        request.getScheduleIds(),
                        CoachAssignmentStatus.ACTIVE
                );

        Set<String> assignedScheduleIds = existingAssignments.stream()
                .map(ca -> ca.getClassSchedule().getScheduleId())
                .collect(Collectors.toSet());

        List<CoachAssignment> coachAssignmentsToSave = new ArrayList<>();

        for (ClassSchedule schedule : schedules) {
            if (assignedScheduleIds.contains(schedule.getScheduleId())) {
                log.warn("Coach {} already assigned to class {}", coach.getUserId(), schedule.getScheduleId());
                throw new AppException(ErrorCode.COACH_ALREADY_ASSIGNED);
            }

            CoachAssignment coachAssignment = coachAssignmentMapper.toEntity(request);
            coachAssignment.setCoach(coach);
            coachAssignment.setClassSchedule(schedule);

            coachAssignmentsToSave.add(coachAssignment);
        }

        List<CoachAssignment> savedAssignments = coachAssignmentRepository.saveAll(coachAssignmentsToSave);

        log.info("Assigned Coach {} to {} classes", coach.getUserId(), savedAssignments.size());

        return savedAssignments;
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "coachAssignments", allEntries = true),
            @CacheEvict(value = "detailedCoachAssignments", allEntries = true),
            @CacheEvict(value = "coachDetail", allEntries = true),

            // Quan trọng: xóa assignment làm thay đổi coaches của lớp
            @CacheEvict(value = "classScheduleDetail", allEntries = true),
            @CacheEvict(value = "classScheduleList", allEntries = true)
    })
    public void deleteCoachAssignment(UUID coachAssignmentId) {
        CoachAssignment coachAssignment = coachAssignmentRepository.findById(coachAssignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.COACH_ASSIGNMENT_NOT_FOUND));

        coachAssignmentRepository.delete(coachAssignment);

        log.info("Deleted CoachAssignment with id {}", coachAssignmentId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "coachAssignments", allEntries = true),
            @CacheEvict(value = "detailedCoachAssignments", allEntries = true),
            @CacheEvict(value = "coachDetail", allEntries = true),

            // Quan trọng: update status/coach/classSchedule có thể đổi coaches của lớp
            @CacheEvict(value = "classScheduleDetail", allEntries = true),
            @CacheEvict(value = "classScheduleList", allEntries = true)
    })
    public void updateCoachAssignment(UUID coachAssignmentId, CoachAssignmentReqDTO.UpdateRequest request) {
        CoachAssignment coachAssignment = coachAssignmentRepository.findById(coachAssignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.COACH_ASSIGNMENT_NOT_FOUND));

        coachAssignmentMapper.updateEntityFromDto(request, coachAssignment);

        log.info("Updated CoachAssignment with id {}", coachAssignmentId);
    }

    /**
     * Cache được vì trả DTO SimpleResponse, không trả Entity.
     */
    @Cacheable(
            value = "coachAssignments",
            key = "#userId.toString() + '_' + #status.name()",
            unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<CoachAssignmentResDTO.SimpleResponse> findCoachAssignmentsByCoachId(
            UUID userId,
            CoachAssignmentStatus status
    ) {
        coachRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        List<CoachAssignment> assignments = coachAssignmentRepository.findByCoach_UserIdAndStatusWithClassSchedule(
                userId,
                status
        );

        if (assignments.isEmpty()) {
            log.info("No CoachAssignments found for Coach ID: {} with status {}", userId, status);
        }

        return assignments.stream()
                .map(coachAssignmentMapper::toSimpleResponse)
                .toList();
    }

    /**
     * Cache được vì trả DTO Response, không trả Entity.
     */
    @Cacheable(
            value = "detailedCoachAssignments",
            key = "#userId.toString() + '_' + #status.name()",
            unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<CoachAssignmentResDTO.Response> findDetailedCoachAssignmentsByUserId(
            UUID userId,
            CoachAssignmentStatus status
    ) {
        coachRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        List<CoachAssignment> assignments = coachAssignmentRepository.findByCoach_UserIdAndStatusWithClassSchedule(
                userId,
                status
        );

        if (assignments.isEmpty()) {
            log.info("No CoachAssignments found for Coach ID: {} with status {}", userId, status);
        }

        return assignments.stream()
                .map(coachAssignmentMapper::toResponse)
                .toList();
    }

    /**
     * Không cần cache. Hàm này lấy toàn bộ assignments theo status,
     * thường dùng cho internal logic/admin export.
     */
    @Transactional(readOnly = true)
    public List<CoachAssignmentResDTO.SimpleResponse> getAllCoachAssignmentsByStatus(CoachAssignmentStatus status) {
        List<CoachAssignment> assignments = coachAssignmentRepository.findByStatus(status);

        if (assignments.isEmpty()) {
            log.info("No CoachAssignments found with status: {}", status);
        }

        return assignments.stream()
                .map(coachAssignmentMapper::toSimpleResponse)
                .toList();
    }
}