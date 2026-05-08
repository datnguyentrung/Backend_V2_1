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

    public List<CoachAssignment> getAllCoachAssignmentsByListCoachIds(List<UUID> coachIds, CoachAssignmentStatus status) {
        return coachAssignmentRepository.findByCoachIdInAndStatusWithClassSchedule(coachIds, status);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            //@CacheEvict(value = "coachAssignments", key = "#request.coachId + '_' + T(com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus).ACTIVE"),
            //@CacheEvict(value = "detailedCoachAssignments", key = "#request.coachId + '_' + T(com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus).ACTIVE"),
            //@CacheEvict(value = "coachDetail", key = "#request.coachId"),
            // ✅ QUAN TRỌNG: Xóa cache chi tiết lớp học vì danh sách HLV của lớp vừa bị thay đổi
            //@CacheEvict(value = "classScheduleDetail", allEntries = true)
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

        log.info("Assigned Coach {} to {} classes", coach.getUserId(), coachAssignmentsToSave.size());
        return coachAssignmentRepository.saveAll(coachAssignmentsToSave);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            //@CacheEvict(value = "coachAssignments", allEntries = true),
            //@CacheEvict(value = "detailedCoachAssignments", allEntries = true),
            //@CacheEvict(value = "coachDetail", allEntries = true),
// ✅ QUAN TRỌNG: Xóa cache chi tiết lớp học
            //@CacheEvict(value = "classScheduleDetail", allEntries = true)
    })

    public void deleteCoachAssignment(UUID coachAssignmentId) {
        CoachAssignment coachAssignment = coachAssignmentRepository.findById(coachAssignmentId)
                .orElseThrow(() -> new IllegalArgumentException("CoachAssignment with id " + coachAssignmentId + " not found"));

        coachAssignmentRepository.delete(coachAssignment);
        log.info("Deleted CoachAssignment with id {}", coachAssignmentId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            //@CacheEvict(value = "coachAssignments", allEntries = true),
            //@CacheEvict(value = "detailedCoachAssignments", allEntries = true),
            //@CacheEvict(value = "coachDetail", allEntries = true),
// ✅ QUAN TRỌNG: Xóa cache chi tiết lớp học
            //@CacheEvict(value = "classScheduleDetail", allEntries = true)
    })

    public void updateCoachAssignment(UUID coachAssignmentId, CoachAssignmentReqDTO.UpdateRequest request) {
        CoachAssignment coachAssignment = coachAssignmentRepository.findById(coachAssignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.COACH_ASSIGNMENT_NOT_FOUND));

        coachAssignmentMapper.updateEntityFromDto(request, coachAssignment);
    }

    /**
     * Tìm tất cả phân công HLV (CoachAssignment) theo ID HLV
     *
     * @param userId ID HLV
     * @return Danh sách phân công HLV đang active dưới dạng SimpleResponse
     */
    //@Cacheable(value = "coachAssignments", key = "#userId.toString() + '_' + #status")
    public List<CoachAssignmentResDTO.SimpleResponse> findCoachAssignmentsByCoachId(UUID userId, CoachAssignmentStatus status) {
        coachRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        List<CoachAssignment> assignments = coachAssignmentRepository.findByCoach_UserIdAndStatusWithClassSchedule(
                userId,
                status
        );
        if (assignments.isEmpty()) {
            log.info("No active CoachAssignments found for Coach ID: {}", userId);
        }

        return assignments.stream()
                .map(coachAssignmentMapper::toSimpleResponse)
                .toList();
    }

    /**
     * Tìm tất cả phân công HLV (CoachAssignment) theo ID HLV - Response đầy đủ
     *
     * @param userId ID HLV
     * @return Danh sách phân công HLV đang active dưới dạng Response đầy đủ
     */
    //@Cacheable(value = "detailedCoachAssignments", key = "#userId.toString() + '_' + #status")
    public List<CoachAssignmentResDTO.Response> findDetailedCoachAssignmentsByUserId(UUID userId, CoachAssignmentStatus status) {
        coachRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        List<CoachAssignment> assignments = coachAssignmentRepository.findByCoach_UserIdAndStatusWithClassSchedule(
                userId,
                status
        );
        if (assignments.isEmpty()) {
            log.info("No active CoachAssignments found for Coach ID: {}", userId);
        }

        return assignments.stream()
                .map(coachAssignmentMapper::toResponse)
                .toList();
    }

    // Hàm này không cần Cache vì nó lấy TOÀN BỘ assignments, thường dùng cho internal logic hoặc admin export.
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