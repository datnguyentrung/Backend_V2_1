package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.domain.Operation.StudentEnrollment;
import com.dat.backend_v2_1.dto.Operation.StudentEnrollmentReqDTO;
import com.dat.backend_v2_1.dto.Operation.StudentEnrollmentResDTO;
import com.dat.backend_v2_1.enums.ErrorCode;
import com.dat.backend_v2_1.enums.Operation.StudentEnrollmentStatus;
import com.dat.backend_v2_1.mapper.Operation.StudentEnrollmentMapper;
import com.dat.backend_v2_1.repository.Core.StudentRepository;
import com.dat.backend_v2_1.repository.Operation.StudentEnrollmentRepository;
import com.dat.backend_v2_1.service.Core.ClassScheduleService;
import com.dat.backend_v2_1.util.error.AppException;
import com.dat.backend_v2_1.util.error.BusinessException;
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
public class StudentEnrollmentService {
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentRepository studentRepository;
    private final ClassScheduleService classScheduleService;
    private final StudentEnrollmentMapper studentEnrollmentMapper;

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            // Danh sách học viên trong lớp thay đổi
            @CacheEvict(value = "studentEnrollmentsByClassDTO", allEntries = true),

            // Quan trọng: enrollment thay đổi totalStudents trong ClassScheduleDetail/List
            @CacheEvict(value = "classScheduleDetail", allEntries = true),
            @CacheEvict(value = "classScheduleList", allEntries = true)
    })
    public List<StudentEnrollment> createStudentEnrollment(StudentEnrollmentReqDTO.CreateRequest request) {
        Student student = studentRepository.findByStudentCode(request.getStudentId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy học viên với ID: " + request.getStudentId()));

        List<ClassSchedule> schedules = classScheduleService.findByScheduleIds(request.getScheduleIds());

        if (schedules.size() != request.getScheduleIds().size()) {
            throw new AppException(ErrorCode.CLASS_NOT_FOUND);
        }

        /**
         * Không dùng cache ở đây.
         * Đây là internal write logic, cần dữ liệu mới nhất từ DB để check trùng enrollment.
         */
        List<StudentEnrollment> currentEnrollments =
                studentEnrollmentRepository.findByStudent_UserIdAndStatusWithClassSchedule(
                        student.getUserId(),
                        StudentEnrollmentStatus.ACTIVE
                );

        Set<String> currentlyEnrolledScheduleIds = currentEnrollments.stream()
                .map(e -> e.getClassSchedule().getScheduleId())
                .collect(Collectors.toSet());

        List<StudentEnrollment> enrollmentsToSave = new ArrayList<>();

        for (ClassSchedule schedule : schedules) {
            if (currentlyEnrolledScheduleIds.contains(schedule.getScheduleId())) {
                log.warn("Student {} already in class {}", student.getUserId(), schedule.getScheduleId());
                throw new AppException(ErrorCode.STUDENT_ALREADY_ENROLLED);
            }

            StudentEnrollment enrollment = studentEnrollmentMapper.toEntity(request);
            enrollment.setStudent(student);
            enrollment.setClassSchedule(schedule);
            enrollment.setStatus(StudentEnrollmentStatus.ACTIVE);

            enrollmentsToSave.add(enrollment);
        }

        List<StudentEnrollment> savedEnrollments = studentEnrollmentRepository.saveAll(enrollmentsToSave);

        log.info("Successfully enrolled student {} to {} classes", student.getUserId(), savedEnrollments.size());

        return savedEnrollments;
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "studentEnrollmentsByClassDTO", allEntries = true),

            // Xóa cache lớp để cập nhật lại totalStudents
            @CacheEvict(value = "classScheduleDetail", allEntries = true),
            @CacheEvict(value = "classScheduleList", allEntries = true)
    })
    public void deleteStudentEnrollment(UUID enrollmentId) {
        if (!studentEnrollmentRepository.existsById(enrollmentId)) {
            throw new AppException(ErrorCode.ENROLLMENT_NOT_FOUND);
        }

        studentEnrollmentRepository.deleteById(enrollmentId);

        log.info("Deleted student enrollment with ID: {}", enrollmentId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "studentEnrollmentsByClassDTO", allEntries = true),

            // Update status ACTIVE/INACTIVE làm thay đổi sĩ số lớp
            @CacheEvict(value = "classScheduleDetail", allEntries = true),
            @CacheEvict(value = "classScheduleList", allEntries = true)
    })
    public void updateStudentEnrollment(UUID enrollmentId, StudentEnrollmentReqDTO.UpdateRequest request) {
        StudentEnrollment enrollment = studentEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

        studentEnrollmentMapper.updateEntityFromDto(request, enrollment);

        log.info("Updated student enrollment with ID: {}", enrollmentId);
    }

    /**
     * Trả Entity nên không cache.
     */
    @Transactional(readOnly = true)
    public List<StudentEnrollment> findStudentEnrollmentsByStudentCode(String studentCode) {
        studentRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new BusinessException("Không tìm thấy học viên với mã: " + studentCode));

        List<StudentEnrollment> enrollments =
                studentEnrollmentRepository.findByStudent_StudentCodeAndStatusWithClassSchedule(
                        studentCode,
                        StudentEnrollmentStatus.ACTIVE
                );

        if (enrollments.isEmpty()) {
            log.info("No active enrollments found for student: {}", studentCode);
        }

        return enrollments;
    }

    /**
     * Trả Entity nên không cache.
     */
    @Transactional(readOnly = true)
    public List<StudentEnrollment> findStudentEnrollmentsByStudentId(UUID userId) {
        studentRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy học viên với mã: " + userId));

        List<StudentEnrollment> enrollments =
                studentEnrollmentRepository.findByStudent_UserIdAndStatusWithClassSchedule(
                        userId,
                        StudentEnrollmentStatus.ACTIVE
                );

        if (enrollments.isEmpty()) {
            log.info("No active enrollments found for student: {}", userId);
        }

        return enrollments;
    }

    /**
     * Trả Entity nên không cache.
     */
    @Transactional(readOnly = true)
    public List<StudentEnrollment> getStudentEnrollmentsByClassScheduleId(String classScheduleId) {
        return studentEnrollmentRepository.findByScheduleIdAndStatusWithStudent(
                classScheduleId,
                StudentEnrollmentStatus.ACTIVE
        );
    }

    /**
     * Cache được vì trả DTO cho UI.
     */
    @Cacheable(
            value = "studentEnrollmentsByClassDTO",
            key = "#classScheduleId",
            unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<StudentEnrollmentResDTO.EnrolledStudentItem> getEnrolledStudentItemsByClass(String classScheduleId) {
        List<StudentEnrollment> enrollments = getStudentEnrollmentsByClassScheduleId(classScheduleId);
        return studentEnrollmentMapper.toEnrolledStudentItemList(enrollments);
    }

    /**
     * Trả Entity nên không cache.
     */
    @Transactional(readOnly = true)
    public StudentEnrollment getEnrollmentByStudentUserIdAndClassScheduleId(
            UUID studentUserId,
            String classScheduleId
    ) {
        return studentEnrollmentRepository.findByStudent_UserIdAndClassSchedule_ScheduleIdAndStatus(
                studentUserId,
                classScheduleId,
                StudentEnrollmentStatus.ACTIVE
        ).orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }
}