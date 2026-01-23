package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.domain.Operation.StudentEnrollment;
import com.dat.backend_v2_1.dto.Operation.StudentEnrollmentReqDTO;
import com.dat.backend_v2_1.dto.Operation.StudentEnrollmentResDTO;
import com.dat.backend_v2_1.enums.Operation.ErrorCode;
import com.dat.backend_v2_1.enums.Operation.StudentEnrollmentStatus;
import com.dat.backend_v2_1.mapper.Operation.StudentEnrollmentMapper;
import com.dat.backend_v2_1.repository.Operation.StudentEnrollmentRepository;
import com.dat.backend_v2_1.service.Core.ClassScheduleService;
import com.dat.backend_v2_1.service.Core.StudentService;
import com.dat.backend_v2_1.util.error.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final StudentService studentService;

    private final ClassScheduleService classScheduleService;

    private final StudentEnrollmentMapper studentEnrollmentMapper;

    @Transactional(rollbackFor = Exception.class)
    // 1. Đổi tên method cho chuẩn (sửa lỗi chính tả Enrollent -> Enrollment)
    // 2. Thay vì trả về String, hãy trả về void (hoặc ID của bản ghi mới tạo)
    public void createStudentEnrollment(StudentEnrollmentReqDTO.CreateRequest request) {
        // 1. Tìm Student (1 lần)
        Student student = studentService.getStudentById(request.getStudentId());

        // 2. Tìm tất cả ClassSchedule theo danh sách ID (1 query thay vì N query)
        List<ClassSchedule> schedules = classScheduleService.findByScheduleIds(request.getScheduleIds());

        // Validation: Kiểm tra xem có lớp nào ID sai không
        if (schedules.size() != request.getScheduleIds().size()) {
            throw new AppException(ErrorCode.CLASS_NOT_FOUND);
        }

        List<StudentEnrollment> enrollmentsToSave = new ArrayList<>();

        // 3. Duyệt qua từng lớp để tạo Enrollment
        for (ClassSchedule schedule : schedules) {

            // Check trùng lặp: Học viên đã học lớp này chưa?
            // Lưu ý: Nếu list quá lớn, query trong vòng for sẽ chậm.
            // Nếu list nhỏ (vài lớp) thì chấp nhận được. Tối ưu hơn thì dùng query IN ở bước trên.
            boolean exists = studentEnrollmentRepository.existsByStudent_UserIdAndClassSchedule_ScheduleIdAndStatus(
                    UUID.fromString(request.getStudentId()),
                    schedule.getScheduleId(),
                    StudentEnrollmentStatus.ACTIVE
            );

            if (exists) {
                log.warn("Student {} already in class {}", student.getUserId(), schedule.getScheduleId());
                // Tùy chọn: Bỏ qua lớp này (continue) hoặc ném lỗi dừng tất cả (throw)
                // Ở đây mình chọn throw lỗi để báo FE biết
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
        studentEnrollmentRepository.saveAll(enrollmentsToSave);

        log.info("Successfully enrolled student {} to {} classes", student.getUserId(), enrollmentsToSave.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteStudentEnrollment(UUID enrollmentId) {
        if (!studentEnrollmentRepository.existsById(enrollmentId)) {
            throw new AppException(ErrorCode.ENROLLMENT_NOT_FOUND);
        }
        studentEnrollmentRepository.deleteById(enrollmentId);
        log.info("Deleted student enrollment with ID: {}", enrollmentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStudentEnrollment(UUID enrollmentId, StudentEnrollmentReqDTO.UpdateRequest request) {
        // 1. Tìm Enrollment
        StudentEnrollment enrollment = studentEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

        // 2. Cập nhật thông tin từ request
        studentEnrollmentMapper.updateEntityFromDto(request, enrollment);
    }

    /**
     * Tìm tất cả các lớp học mà học viên đang tham gia (trạng thái ACTIVE)
     * @param userId ID của học viên
     * @return Danh sách các enrollment đang active
     */
    public List<StudentEnrollmentResDTO.SimpleResponse> findStudentEnrollmentsByUserId(UUID userId) {
        // Validate student exists
        studentService.getStudentById(userId);

        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByStudent_UserIdAndStatusWithClassSchedule(
                userId,
                StudentEnrollmentStatus.ACTIVE
        );

        if (enrollments.isEmpty()) {
            log.info("No active enrollments found for student: {}", userId);
        }

        return enrollments.stream()
                .map(studentEnrollmentMapper::toSimpleResponse)
                .toList();
    }

    /**
     * Tìm tất cả các lớp học mà học viên đang tham gia (trạng thái ACTIVE) - Response đầy đủ
     * @param userId ID của học viên
     * @return Danh sách các enrollment đầy đủ thông tin
     */
    public List<StudentEnrollmentResDTO.Response> findDetailedStudentEnrollmentsByUserId(UUID userId) {
        // Validate student exists
        studentService.getStudentById(userId);

        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByStudent_UserIdAndStatusWithClassSchedule(
                userId,
                StudentEnrollmentStatus.ACTIVE
        );

        if (enrollments.isEmpty()) {
            log.info("No active enrollments found for student: {}", userId);
        }

        return enrollments.stream()
                .map(studentEnrollmentMapper::toResponse)
                .toList();
    }
}
