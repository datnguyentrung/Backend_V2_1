package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Operation.CoachAssignment;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentReqDTO;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.dat.backend_v2_1.enums.ErrorCode;
import com.dat.backend_v2_1.mapper.Operation.CoachAssignmentMapper;
import com.dat.backend_v2_1.repository.Operation.CoachAssignmentRepository;
import com.dat.backend_v2_1.service.Core.ClassScheduleService;
import com.dat.backend_v2_1.service.Core.CoachService;
import com.dat.backend_v2_1.util.error.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoachAssignmentService {
    private final CoachAssignmentRepository coachAssignmentRepository;

    private final CoachService coachService;
    private final ClassScheduleService classScheduleService;
    private final CoachAssignmentMapper coachAssignmentMapper;

    public void createdCoachAssignment(CoachAssignmentReqDTO.CreateRequest request) {
        // 1. Lấy thông tin HLV
        Coach coach = coachService.getCoachById(request.getCoachId());

        // 2. Lấy tất cả ClassSchedule theo danh sách ID
        List<ClassSchedule> schedules = classScheduleService.findByScheduleIds(request.getScheduleIds());

        // Validation: Kiểm tra xem có lớp nào ID sai không
        if (schedules.size() != request.getScheduleIds().size()) {
            throw new IllegalArgumentException("One or more ClassSchedule IDs are invalid");
        }

        // 3. Kiểm tra phân công đã tồn tại - FIX N+1: Lấy tất cả assignments trong 1 query
        List<CoachAssignment> existingAssignments = coachAssignmentRepository
                .findByCoachAndScheduleIdsAndStatus(
                        coach.getUserId(),
                        request.getScheduleIds(),
                        CoachAssignmentStatus.ACTIVE
                );

        // Tạo Set chứa scheduleId đã được phân công để tra cứu nhanh O(1)
        Set<String> assignedScheduleIds = existingAssignments.stream()
                .map(ca -> ca.getClassSchedule().getScheduleId())
                .collect(Collectors.toSet());

        List<CoachAssignment> coachAssignmentsToSave = new ArrayList<>();

        // 4. Duyệt qua từng lớp để tạo CoachAssignment
        for (ClassSchedule schedule : schedules) {
            // Kiểm tra xem schedule đã được phân công chưa
            if (assignedScheduleIds.contains(schedule.getScheduleId())) {
                log.warn("Coach {} already assigned to class {}", coach.getUserId(), schedule.getScheduleId());
                throw new AppException(ErrorCode.COACH_ALREADY_ASSIGNED);
            }

            CoachAssignment coachAssignment = coachAssignmentMapper.toEntity(request);

            coachAssignment.setCoach(coach);
            coachAssignment.setClassSchedule(schedule);

            coachAssignmentsToSave.add(coachAssignment);
        }

        // 5. Lưu tất cả phân công HLV cùng lúc
        coachAssignmentRepository.saveAll(coachAssignmentsToSave);

        log.info("Assigned Coach {} to {} classes", coach.getUserId(), coachAssignmentsToSave.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCoachAssignment(UUID coachAssignmentId){
        CoachAssignment coachAssignment = coachAssignmentRepository.findById(coachAssignmentId)
                .orElseThrow(() -> new IllegalArgumentException("CoachAssignment with id " + coachAssignmentId + " not found"));

        coachAssignmentRepository.delete(coachAssignment);
        log.info("Deleted CoachAssignment with id {}", coachAssignmentId);
    }

    public void updateCoachAssignment(UUID coachAssignmentId, CoachAssignmentReqDTO.UpdateRequest request) {
        // 1. Tìm CoachAssignment
        CoachAssignment coachAssignment = coachAssignmentRepository.findById(coachAssignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.COACH_ASSIGNMENT_NOT_FOUND));

        // 2. Cập nhật các trường nếu có trong request
        coachAssignmentMapper.updateEntityFromDto(request,coachAssignment);
    }

    /**
     * Tìm tất cả phân công HLV (CoachAssignment) theo ID HLV
     * @param userId ID HLV
     * @return Danh sách phân công HLV đang active dưới dạng SimpleResponse
     */
    public List<CoachAssignmentResDTO.SimpleResponse> findStudentEnrollmentsByCoachId(UUID userId){
        // Validation: Kiểm tra HLV tồn tại
        coachService.getCoachById(userId);

        List<CoachAssignment> assignments = coachAssignmentRepository.findByCoach_UserIdAndStatusWithClassSchedule(
                userId,
                CoachAssignmentStatus.ACTIVE
        );
        if (assignments.isEmpty()){
            log.info("No active CoachAssignments found for Coach ID: {}", userId);
        }

        return assignments.stream()
                .map(coachAssignmentMapper::toSimpleResponse)
                .toList();
    }

    /**
     * Tìm tất cả phân công HLV (CoachAssignment) theo ID HLV - Response đầy đủ
     * @param userId ID HLV
     * @return Danh sách phân công HLV đang active dưới dạng Response đầy đủ
     */
    public List<CoachAssignmentResDTO.Response> findDetailedCoachAssignmentsByUserId(UUID userId){
        // Validation: Kiểm tra HLV tồn tại
        coachService.getCoachById(userId);

        List<CoachAssignment> assignments = coachAssignmentRepository.findByCoach_UserIdAndStatusWithClassSchedule(
                userId,
                CoachAssignmentStatus.ACTIVE
        );
        if (assignments.isEmpty()){
            log.info("No active CoachAssignments found for Coach ID: {}", userId);
        }

        return assignments.stream()
                .map(coachAssignmentMapper::toResponse)
                .toList();
    }
}
