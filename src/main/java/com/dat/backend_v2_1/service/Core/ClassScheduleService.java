package com.dat.backend_v2_1.service.Core;

import com.dat.backend_v2_1.domain.Core.Branch;
import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Operation.CoachAssignment;
import com.dat.backend_v2_1.dto.Core.ClassScheduleReqDTO;
import com.dat.backend_v2_1.dto.Core.ClassScheduleResDTO;
import com.dat.backend_v2_1.dto.Core.CoachResDTO;
import com.dat.backend_v2_1.enums.ErrorCode;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.dat.backend_v2_1.enums.Operation.StudentEnrollmentStatus;
import com.dat.backend_v2_1.mapper.Core.ClassScheduleMapper;
import com.dat.backend_v2_1.repository.Core.ClassScheduleRepository;
import com.dat.backend_v2_1.repository.Operation.CoachAssignmentRepository;
import com.dat.backend_v2_1.repository.Operation.StudentEnrollmentRepository;
import com.dat.backend_v2_1.util.error.AppException;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassScheduleService {
    private final ClassScheduleRepository classScheduleRepository;
    private final ClassScheduleMapper classScheduleMapper;
    private final CoachAssignmentRepository coachAssignmentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final BranchService branchService;

    // ========== READ OPERATIONS ==========

    public ClassSchedule getClassScheduleById(String scheduleId) {
        return classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> {
                    log.error("Class schedule not found with id: {}", scheduleId);
                    return new AppException(ErrorCode.CLASS_NOT_FOUND);
                });
    }

    public ClassScheduleResDTO.ClassScheduleDetail getClassScheduleDetail(String scheduleId) {
        ClassSchedule schedule = getClassScheduleById(scheduleId);
        List<CoachAssignment> coachAssignments = coachAssignmentRepository
                .findByClassSchedule_ScheduleIdAndStatus(scheduleId, CoachAssignmentStatus.ACTIVE);
        ClassScheduleResDTO.ClassScheduleDetail detail = classScheduleMapper.toClassScheduleDetail(schedule, coachAssignments);
        long studentCount = studentEnrollmentRepository.countByClassSchedule_ScheduleIdAndStatus(
                scheduleId, StudentEnrollmentStatus.ACTIVE);
        detail.setTotalStudents((int) studentCount);
        return detail;
    }

    public List<ClassScheduleResDTO.ClassScheduleDetail> getAllClassSchedules() {
        List<ClassSchedule> schedules = classScheduleRepository.findAll();
        if (schedules.isEmpty()) {
            log.info("No class schedules found");
            return Collections.emptyList();
        }

        List<CoachAssignment> coachAssignments = coachAssignmentRepository.findByStatus(CoachAssignmentStatus.ACTIVE);
        Map<String, List<CoachResDTO.CoachSummary>> scheduleIdToCoaches = coachAssignments.stream()
                .collect(Collectors.groupingBy(
                        ca -> ca.getClassSchedule().getScheduleId(),
                        Collectors.mapping(ca -> {
                            Coach coach = ca.getCoach();
                            return CoachResDTO.CoachSummary.builder()
                                    .userId(coach.getUserId())
                                    .fullName(coach.getFullName())
                                    .staffCode(coach.getStaffCode())
                                    .build();
                        }, Collectors.toList())
                ));

        Map<String, Long> studentCountMap = studentEnrollmentRepository.findAll().stream()
                .filter(enrollment -> enrollment.getStatus() == StudentEnrollmentStatus.ACTIVE)
                .collect(Collectors.groupingBy(
                        enrollment -> enrollment.getClassSchedule().getScheduleId(),
                        Collectors.counting()
                ));

        return schedules.stream()
                .map(schedule -> {
                    ClassScheduleResDTO.ClassScheduleDetail detail = classScheduleMapper.toClassScheduleDetail(schedule);
                    detail.setCoaches(scheduleIdToCoaches.getOrDefault(schedule.getScheduleId(), Collections.emptyList()));
                    detail.setTotalStudents(studentCountMap.getOrDefault(schedule.getScheduleId(), 0L).intValue());
                    return detail;
                })
                .collect(Collectors.toList());
    }

    public List<ClassSchedule> findByScheduleIds(List<String> scheduleIds) {
        if (CollectionUtils.isEmpty(scheduleIds)) {
            return Collections.emptyList();
        }
        return classScheduleRepository.findAllById(scheduleIds);
    }

    // ========== CREATE OPERATION ==========

    @Transactional(rollbackFor = Exception.class)
    public ClassScheduleResDTO.ClassScheduleDetail createClassSchedule(ClassScheduleReqDTO.CreateRequest request) {
        if (classScheduleRepository.existsById(request.getScheduleId())) {
            throw new AppException(ErrorCode.CLASS_ALREADY_EXISTS);
        }

        Branch branch = branchService.getBranchById(request.getBranchId());

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }

        ClassSchedule classSchedule = classScheduleMapper.toEntity(request);
        classSchedule.setBranch(branch);

        try {
            ClassSchedule savedSchedule = classScheduleRepository.save(classSchedule);
            log.info("Created new class schedule with ID: {}", savedSchedule.getScheduleId());
            return classScheduleMapper.toClassScheduleDetail(savedSchedule);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation: {}", e.getMessage());
            throw new AppException(ErrorCode.CLASS_ALREADY_EXISTS);
        }
    }

    // ========== UPDATE OPERATION ==========

    @Transactional(rollbackFor = Exception.class)
    public ClassScheduleResDTO.ClassScheduleDetail updateClassSchedule(String scheduleId, ClassScheduleReqDTO.UpdateRequest request) {
        ClassSchedule classSchedule = getClassScheduleById(scheduleId);

        if (request.getBranchId() != null) {
            Branch branch = branchService.getBranchById(request.getBranchId());
            classSchedule.setBranch(branch);
        }

        LocalTime newStartTime = request.getStartTime() != null ? request.getStartTime() : classSchedule.getStartTime();
        LocalTime newEndTime = request.getEndTime() != null ? request.getEndTime() : classSchedule.getEndTime();

        if (!newEndTime.isAfter(newStartTime)) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }

        classScheduleMapper.updateEntityFromDto(request, classSchedule);
        classScheduleRepository.save(classSchedule);
        log.info("Updated class schedule: {}", scheduleId);

        return getClassScheduleDetail(scheduleId);
    }

    // ========== DELETE OPERATION ==========

    @Transactional(rollbackFor = Exception.class)
    public void deleteClassSchedule(String scheduleId) {
        ClassSchedule classSchedule = getClassScheduleById(scheduleId);

        long enrollmentCount = studentEnrollmentRepository.countByClassSchedule_ScheduleIdAndStatus(
                scheduleId, StudentEnrollmentStatus.ACTIVE);
        if (enrollmentCount > 0) {
            throw new AppException(ErrorCode.CLASS_HAS_STUDENTS);
        }

        long assignmentCount = coachAssignmentRepository.countByClassSchedule_ScheduleIdAndStatus(
                scheduleId, CoachAssignmentStatus.ACTIVE);
        if (assignmentCount > 0) {
            throw new AppException(ErrorCode.CLASS_HAS_COACHES);
        }

        classScheduleRepository.delete(classSchedule);
        log.info("Deleted class schedule: {}", scheduleId);
    }
}
