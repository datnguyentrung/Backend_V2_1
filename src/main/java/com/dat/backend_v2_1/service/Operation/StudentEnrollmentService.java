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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
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

    @Autowired
    @Lazy
    private StudentEnrollmentService self; // Dùng để gọi các hàm có @Cacheable nội bộ

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "studentEnrollmentsById", key = "#request.studentId"),
            @CacheEvict(value = "studentEnrollmentsByCode", allEntries = true),
            @CacheEvict(value = "studentEnrollmentsByClass", allEntries = true),
            // QUAN TRỌNG: Xóa cache chi tiết Lớp học để hệ thống tính toán lại tổng số sinh viên (totalStudents)
            @CacheEvict(value = "classScheduleDetail", allEntries = true)
    })
    public List<StudentEnrollment> createStudentEnrollment(StudentEnrollmentReqDTO.CreateRequest request) {
        // 1. Tìm Student (1 lần)
        Student student = studentRepository.findByStudentCode(request.getStudentId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy học viên với ID: " + request.getStudentId()));

        // 2. Tìm tất cả ClassSchedule theo danh sách ID (1 query thay vì N query)
        List<ClassSchedule> schedules = classScheduleService.findByScheduleIds(request.getScheduleIds());

        // Validation: Kiểm tra xem có lớp nào ID sai không
        if (schedules.size() != request.getScheduleIds().size()) {
            throw new AppException(ErrorCode.CLASS_NOT_FOUND);
        }

        // [TỐI ƯU N+1]: Tận dụng hàm Cache để lấy danh sách các lớp hiện tại của sinh viên
        List<StudentEnrollment> currentEnrollments = self.findStudentEnrollmentsByStudentId(student.getUserId());
        Set<String> currentlyEnrolledScheduleIds = currentEnrollments.stream()
                .map(e -> e.getClassSchedule().getScheduleId())
                .collect(Collectors.toSet());

        List<StudentEnrollment> enrollmentsToSave = new ArrayList<>();

        // 3. Duyệt qua từng lớp để tạo Enrollment
        for (ClassSchedule schedule : schedules) {
            // Check trùng lặp: Tra cứu O(1) từ Set thay vì chọc xuống DB liên tục
            if (currentlyEnrolledScheduleIds.contains(schedule.getScheduleId())) {
                log.warn("Student {} already in class {}", student.getUserId(), schedule.getScheduleId());
                throw new AppException(ErrorCode.STUDENT_ALREADY_ENROLLED);
            }

            // Dùng Mapper tạo object cơ bản (có joinDate, note...)
            StudentEnrollment enrollment = studentEnrollmentMapper.toEntity(request);

            // Set các quan hệ
            enrollment.setStudent(student);
            enrollment.setClassSchedule(schedule);
            enrollment.setStatus(StudentEnrollmentStatus.ACTIVE);

            enrollmentsToSave.add(enrollment);
        }

        // 4. Lưu tất cả một lúc (Bulk Insert)
        List<StudentEnrollment> savedEnrollments = studentEnrollmentRepository.saveAll(enrollmentsToSave);

        log.info("Successfully enrolled student {} to {} classes", student.getUserId(), savedEnrollments.size());

        return savedEnrollments;
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "studentEnrollmentsById", allEntries = true),
            @CacheEvict(value = "studentEnrollmentsByCode", allEntries = true),
            @CacheEvict(value = "studentEnrollmentsByClass", allEntries = true),
            @CacheEvict(value = "singleEnrollment", allEntries = true),
            // Xóa cache chi tiết lớp học để update lại sĩ số
            @CacheEvict(value = "classScheduleDetail", allEntries = true)
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
            @CacheEvict(value = "studentEnrollmentsById", allEntries = true),
            @CacheEvict(value = "studentEnrollmentsByCode", allEntries = true),
            @CacheEvict(value = "studentEnrollmentsByClass", allEntries = true),
            @CacheEvict(value = "singleEnrollment", allEntries = true),
            // Trạng thái enrollment có thể thay đổi (vd Active -> Inactive), ảnh hưởng sĩ số lớp
            @CacheEvict(value = "classScheduleDetail", allEntries = true)
    })
    public void updateStudentEnrollment(UUID enrollmentId, StudentEnrollmentReqDTO.UpdateRequest request) {
        // 1. Tìm Enrollment (Gọi thẳng Repo để đảm bảo data mới nhất trước khi update)
        StudentEnrollment enrollment = studentEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

        // 2. Cập nhật thông tin từ request
        studentEnrollmentMapper.updateEntityFromDto(request, enrollment);
    }

    /**
     * Tìm tất cả các lớp học mà học viên đang tham gia (trạng thái ACTIVE)
     */
    @Cacheable(value = "studentEnrollmentsByCode", key = "#studentCode")
    public List<StudentEnrollment> findStudentEnrollmentsByStudentCode(String studentCode) {
        // Validate student exists
        studentRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new BusinessException("Không tìm thấy học viên với mã: " + studentCode));

        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByStudent_StudentCodeAndStatusWithClassSchedule(
                studentCode,
                StudentEnrollmentStatus.ACTIVE
        );

        if (enrollments.isEmpty()) {
            log.info("No active enrollments found for student: {}", studentCode);
        }

        return enrollments;
    }

    /**
     * Tìm tất cả các lớp học mà học viên đang tham gia (trạng thái ACTIVE) theo userId
     */
    @Cacheable(value = "studentEnrollmentsById", key = "#userId")
    public List<StudentEnrollment> findStudentEnrollmentsByStudentId(UUID userId) {
        // Validate student exists
        studentRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy học viên với mã: " + userId));

        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByStudent_UserIdAndStatusWithClassSchedule(
                userId,
                StudentEnrollmentStatus.ACTIVE
        );

        if (enrollments.isEmpty()) {
            log.info("No active enrollments found for student: {}", userId);
        }

        return enrollments;
    }

    /**
     * Lấy danh sách học viên theo ID lịch học lớp
     */
    public List<StudentEnrollment> getStudentEnrollmentsByClassScheduleId(String classScheduleId) {
        return studentEnrollmentRepository.findByScheduleIdAndStatusWithStudent(
                classScheduleId,
                StudentEnrollmentStatus.ACTIVE
        );
    }

    @Cacheable(value = "studentEnrollmentsByClassDTO", key = "#classScheduleId")
    public List<StudentEnrollmentResDTO.EnrolledStudentItem> getEnrolledStudentItemsByClass(String classScheduleId) {
        List<StudentEnrollment> enrollments = getStudentEnrollmentsByClassScheduleId(classScheduleId);
        return studentEnrollmentMapper.toEnrolledStudentItemList(enrollments);
    }

    @Cacheable(value = "singleEnrollment", key = "#studentUserId.toString() + '_' + #classScheduleId")
    public StudentEnrollment getEnrollmentByStudentUserIdAndClassScheduleId(UUID studentUserId, String classScheduleId) {
        return studentEnrollmentRepository.findByStudent_UserIdAndClassSchedule_ScheduleIdAndStatus(
                studentUserId,
                classScheduleId,
                StudentEnrollmentStatus.ACTIVE
        ).orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }
}