package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.domain.Core.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Core.Student;
import com.dat.ai_receptionist_web.domain.Operation.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Operation.StudentEnrollmentReqDTO;
import com.dat.ai_receptionist_web.dto.Operation.StudentEnrollmentResDTO;
import com.dat.ai_receptionist_web.enums.ErrorCode;
import com.dat.ai_receptionist_web.enums.Operation.StudentEnrollmentStatus;
import com.dat.ai_receptionist_web.mapper.Operation.StudentEnrollmentMapper;
import com.dat.ai_receptionist_web.repository.Core.StudentRepository;
import com.dat.ai_receptionist_web.repository.Operation.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Core.ClassScheduleService;
import com.dat.ai_receptionist_web.util.error.AppException;
import com.dat.ai_receptionist_web.util.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        Student student = studentRepository.findByStudentCode(request.getStudentCode())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        return createStudentEnrollment(student, request, false);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "studentEnrollmentsByClassDTO", allEntries = true),
            @CacheEvict(value = "classScheduleDetail", allEntries = true),
            @CacheEvict(value = "classScheduleList", allEntries = true)
    })
    public List<StudentEnrollment> createStudentEnrollmentForNewStudent(
            Student student,
            StudentEnrollmentReqDTO.CreateRequest request
    ) {
        return createStudentEnrollment(student, request, true);
    }

    private List<StudentEnrollment> createStudentEnrollment(
            Student student,
            StudentEnrollmentReqDTO.CreateRequest request,
            boolean skipExistingEnrollmentCheck
    ) {
        List<String> distinctScheduleIds = request.getScheduleIds().stream()
                .distinct()
                .toList();

        if (distinctScheduleIds.size() != request.getScheduleIds().size()) {
            throw new AppException(ErrorCode.STUDENT_ALREADY_ENROLLED);
        }

        List<ClassSchedule> schedules = classScheduleService.findByScheduleIds(distinctScheduleIds);

        if (schedules.size() != distinctScheduleIds.size()) {
            throw new AppException(ErrorCode.CLASS_NOT_FOUND);
        }

        if (!skipExistingEnrollmentCheck) {
            List<String> alreadyEnrolledScheduleIds =
                    studentEnrollmentRepository.findActiveScheduleIdsByStudentPersonIdAndScheduleIds(
                        student.getPersonId(),
                        distinctScheduleIds,
                        StudentEnrollmentStatus.ACTIVE
                );

            if (!alreadyEnrolledScheduleIds.isEmpty()) {
                log.warn("Student {} already in classes {}", student.getPersonId(), alreadyEnrolledScheduleIds);
                throw new AppException(ErrorCode.STUDENT_ALREADY_ENROLLED);
            }
        }

        List<StudentEnrollment> enrollmentsToSave = new ArrayList<>();

        for (ClassSchedule schedule : schedules) {
            StudentEnrollment enrollment = studentEnrollmentMapper.toEntity(request);
            enrollment.setStudent(student);
            enrollment.setClassSchedule(schedule);
            enrollment.setStatus(StudentEnrollmentStatus.ACTIVE);

            enrollmentsToSave.add(enrollment);
        }

        List<StudentEnrollment> savedEnrollments = studentEnrollmentRepository.saveAll(enrollmentsToSave);

        log.info("Successfully enrolled student {} to {} classes", student.getPersonId(), savedEnrollments.size());

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
                studentEnrollmentRepository.findByStudent_PersonIdAndStatusWithClassSchedule(
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
            unless = "#result == null || #result.isEmpty()",
            cacheManager = "redisCacheManager"
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
        return studentEnrollmentRepository.findByStudent_PersonIdAndClassSchedule_ScheduleIdAndStatus(
                studentUserId,
                classScheduleId,
                StudentEnrollmentStatus.ACTIVE
        ).orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }
}
