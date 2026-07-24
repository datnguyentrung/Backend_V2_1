package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Branch;
import com.dat.ai_receptionist_web.domain.Core.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Core.Coach;
import com.dat.ai_receptionist_web.domain.Operation.CoachAssignment;
import com.dat.ai_receptionist_web.dto.Core.ClassScheduleReqDTO;
import com.dat.ai_receptionist_web.dto.Core.ClassScheduleResDTO;
import com.dat.ai_receptionist_web.dto.Core.CoachResDTO;
import com.dat.ai_receptionist_web.enums.Core.*;
import com.dat.ai_receptionist_web.enums.Core.*;
import com.dat.ai_receptionist_web.enums.ErrorCode;
import com.dat.ai_receptionist_web.enums.Operation.CoachAssignmentStatus;
import com.dat.ai_receptionist_web.enums.Operation.StudentEnrollmentStatus;
import com.dat.ai_receptionist_web.mapper.Core.ClassScheduleMapper;
import com.dat.ai_receptionist_web.repository.Core.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Operation.CoachAssignmentRepository;
import com.dat.ai_receptionist_web.repository.Operation.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.util.error.AppException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

    /**
     * Không cache Entity JPA.
     * Entity có thể dính lazy proxy, persistence context, stale state.
     */
    @Transactional(readOnly = true)
    public ClassSchedule getClassScheduleById(String scheduleId) {
        return classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> {
                    log.error("Class schedule not found with id: {}", scheduleId);
                    return new AppException(ErrorCode.CLASS_NOT_FOUND);
                });
    }

    /**
     * Cache DTO detail theo scheduleId.
     *
     * Lưu ý:
     * - Nếu thay đổi ClassSchedule thì file này đã evict.
     * - Nếu thay đổi CoachAssignment hoặc StudentEnrollment ở service khác,
     *   service đó cũng phải evict cache này theo scheduleId.
     */
    @Cacheable(value = "classScheduleDetail", key = "#scheduleId", unless = "#result == null")
    @Transactional(readOnly = true)
    public ClassScheduleResDTO.ClassScheduleDetail getClassScheduleDetail(String scheduleId) {
        ClassSchedule schedule = getClassScheduleById(scheduleId);
        return buildClassScheduleDetail(schedule);
    }

    /**
     * Có thể cache getAll vì hàm này khá nặng:
     * - query schedules theo filter
     * - query active coach assignments
     * - query toàn bộ enrollments rồi group count
     *
     * Dùng cache riêng classScheduleList.
     * Default key của Spring sẽ lấy toàn bộ params:
     * branchId, weekday, scheduleLevel, scheduleShift, scheduleLocation, scheduleStatus, scheduleIds.
     *
     * Không cache empty list để tránh giữ quá nhiều cache rỗng theo filter linh tinh.
     */
    @Cacheable(value = "classScheduleList", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<ClassScheduleResDTO.ClassScheduleDetail> filterClassSchedules(
            Long branchId,
            Weekday weekday,
            ScheduleLevel scheduleLevel,
            ScheduleShift scheduleShift,
            ScheduleLocation scheduleLocation,
            ScheduleStatus scheduleStatus,
            List<String> scheduleIds) {

        if (scheduleIds != null && scheduleIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ClassSchedule> schedules = classScheduleRepository.findAllWithFilters(
                branchId,
                weekday,
                scheduleLevel,
                scheduleShift,
                scheduleLocation,
                scheduleStatus,
                scheduleIds
        );

        if (schedules.isEmpty()) {
            log.info("No class schedules found matching the filters");
            return Collections.emptyList();
        }

        List<CoachAssignment> coachAssignments = coachAssignmentRepository.findByStatus(CoachAssignmentStatus.ACTIVE);

        Map<String, List<CoachResDTO.CoachSummary>> scheduleIdToCoaches = coachAssignments.stream()
                .filter(ca -> ca.getClassSchedule() != null && ca.getCoach() != null)
                .collect(Collectors.groupingBy(
                        ca -> ca.getClassSchedule().getScheduleId(),
                        Collectors.mapping(ca -> {
                            Coach coach = ca.getCoach();
                            return CoachResDTO.CoachSummary.builder()
                                    .personId(coach.getPersonId())
                                    .fullName(coach.getFullName())
                                    .staffCode(coach.getStaffCode())
                                    .build();
                        }, Collectors.toList())
                ));

        Map<String, Long> studentCountMap = studentEnrollmentRepository.findAll().stream()
                .filter(enrollment ->
                        enrollment.getStatus() == StudentEnrollmentStatus.ACTIVE
                                && enrollment.getClassSchedule() != null
                )
                .collect(Collectors.groupingBy(
                        enrollment -> enrollment.getClassSchedule().getScheduleId(),
                        Collectors.counting()
                ));

        return schedules.stream()
                .map(schedule -> {
                    ClassScheduleResDTO.ClassScheduleDetail detail =
                            classScheduleMapper.toClassScheduleDetail(schedule);

                    detail.setCoaches(
                            scheduleIdToCoaches.getOrDefault(
                                    schedule.getScheduleId(),
                                    Collections.emptyList()
                            )
                    );

                    detail.setTotalStudents(
                            studentCountMap.getOrDefault(schedule.getScheduleId(), 0L).intValue()
                    );

                    return detail;
                })
                .collect(Collectors.toList());
    }

    /**
     * Không cache Entity list.
     */
    @Transactional(readOnly = true)
    public List<ClassSchedule> findByScheduleIds(List<String> scheduleIds) {
        if (CollectionUtils.isEmpty(scheduleIds)) {
            return Collections.emptyList();
        }
        return classScheduleRepository.findAllById(scheduleIds);
    }

    // ========== CREATE OPERATION ==========

    /**
     * Tạo mới lịch học làm thay đổi danh sách getAll theo nhiều filter khác nhau,
     * nên phải xoá toàn bộ classScheduleList.
     *
     * Xoá thêm classScheduleDetail theo scheduleId để phòng trường hợp cache cũ còn sót.
     */
    @Caching(evict = {
            @CacheEvict(value = "classScheduleDetail", key = "#request.scheduleId"),
            @CacheEvict(value = "classScheduleList", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public ClassScheduleResDTO.ClassScheduleDetail createClassSchedule(
            ClassScheduleReqDTO.CreateRequest request
    ) {
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

            return buildClassScheduleDetail(savedSchedule);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation: {}", e.getMessage());
            throw new AppException(ErrorCode.CLASS_ALREADY_EXISTS);
        }
    }

    // ========== UPDATE OPERATION ==========

    /**
     * Update 1 schedule:
     * - xoá cache detail của schedule đó
     * - xoá toàn bộ list cache vì schedule có thể đổi branch/weekday/level/shift/location/status
     */
    @Caching(evict = {
            @CacheEvict(value = "classScheduleDetail", key = "#scheduleId"),
            @CacheEvict(value = "classScheduleList", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public ClassScheduleResDTO.ClassScheduleDetail updateClassSchedule(
            String scheduleId,
            ClassScheduleReqDTO.UpdateRequest request
    ) throws JsonProcessingException {
        ClassSchedule classSchedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        if (request.getBranchId() != null) {
            Branch branch = branchService.getBranchById(request.getBranchId());
            classSchedule.setBranch(branch);
        }

        LocalTime newStartTime = request.getStartTime() != null
                ? request.getStartTime()
                : classSchedule.getStartTime();

        LocalTime newEndTime = request.getEndTime() != null
                ? request.getEndTime()
                : classSchedule.getEndTime();

        if (!newEndTime.isAfter(newStartTime)) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }

        classScheduleMapper.updateEntityFromDto(request, classSchedule);
        ClassSchedule savedSchedule = classScheduleRepository.save(classSchedule);

        log.info("Updated class schedule: {}", scheduleId);

        return buildClassScheduleDetail(savedSchedule);
    }

    // ========== DELETE OPERATION ==========

    /**
     * Delete schedule:
     * - xoá detail cache của schedule đó
     * - xoá toàn bộ list cache
     */
    @Caching(evict = {
            @CacheEvict(value = "classScheduleDetail", key = "#scheduleId"),
            @CacheEvict(value = "classScheduleList", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void deleteClassSchedule(String scheduleId) throws JsonProcessingException {
        ClassSchedule classSchedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        long enrollmentCount = studentEnrollmentRepository.countByClassSchedule_ScheduleIdAndStatus(
                scheduleId,
                StudentEnrollmentStatus.ACTIVE
        );

        if (enrollmentCount > 0) {
            throw new AppException(ErrorCode.CLASS_HAS_STUDENTS);
        }

        long assignmentCount = coachAssignmentRepository.countByClassSchedule_ScheduleIdAndStatus(
                scheduleId,
                CoachAssignmentStatus.ACTIVE
        );

        if (assignmentCount > 0) {
            throw new AppException(ErrorCode.CLASS_HAS_COACHES);
        }

        classScheduleRepository.delete(classSchedule);
        log.info("Deleted class schedule: {}", scheduleId);
    }

    // ========== UPDATE STATUS ==========

    /**
     * Đổi status cũng ảnh hưởng getAll filter theo scheduleStatus,
     * nên phải xoá detail và list cache.
     */
    @Caching(evict = {
            @CacheEvict(value = "classScheduleDetail", key = "#scheduleId"),
            @CacheEvict(value = "classScheduleList", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String scheduleId, ScheduleStatus status) {
        ClassSchedule classSchedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        classSchedule.setScheduleStatus(status);

        log.info("Updated status of class schedule {} to {}", scheduleId, status);
    }

    // ========== PRIVATE HELPERS ==========

    private ClassScheduleResDTO.ClassScheduleDetail buildClassScheduleDetail(ClassSchedule schedule) {
        String scheduleId = schedule.getScheduleId();

        List<CoachAssignment> coachAssignments = coachAssignmentRepository
                .findByClassSchedule_ScheduleIdAndStatus(scheduleId, CoachAssignmentStatus.ACTIVE);

        ClassScheduleResDTO.ClassScheduleDetail detail =
                classScheduleMapper.toClassScheduleDetail(schedule, coachAssignments);

        long studentCount = studentEnrollmentRepository.countByClassSchedule_ScheduleIdAndStatus(
                scheduleId,
                StudentEnrollmentStatus.ACTIVE
        );

        detail.setTotalStudents((int) studentCount);

        return detail;
    }
}
