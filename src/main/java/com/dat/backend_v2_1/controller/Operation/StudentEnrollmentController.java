package com.dat.backend_v2_1.controller.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Operation.StudentEnrollment;
import com.dat.backend_v2_1.dto.Operation.StudentEnrollmentReqDTO;
import com.dat.backend_v2_1.dto.Operation.StudentEnrollmentResDTO;
import com.dat.backend_v2_1.mapper.Core.ClassScheduleMapper;
import com.dat.backend_v2_1.mapper.Operation.StudentEnrollmentMapper;
import com.dat.backend_v2_1.service.Core.ClassScheduleService;
import com.dat.backend_v2_1.service.Operation.StudentEnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý đăng ký học viên vào lớp (Student Enrollment)
 * Xử lý các thao tác CRUD và tra cứu thông tin enrollment
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student-enrollments")
public class StudentEnrollmentController {

    private final StudentEnrollmentService studentEnrollmentService;

    private final StudentEnrollmentMapper studentEnrollmentMapper;

    private final ClassScheduleMapper classScheduleMapper;
    
    private final ClassScheduleService classScheduleService;

    /**
     * Đăng ký học viên vào lớp học
     * POST /api/v1/student-enrollments
     * <p>
     * Cho phép đăng ký một học viên vào một hoặc nhiều lớp học cùng lúc.
     * Hệ thống sẽ kiểm tra trùng lặp và validate thông tin trước khi tạo enrollment.
     *
     * @param request Thông tin đăng ký (studentId, scheduleIds, joinDate, note)
     * @return 201 Created - Đăng ký thành công
     * 400 Bad Request - Dữ liệu không hợp lệ
     * 404 Not Found - Không tìm thấy học viên hoặc lớp học
     * 409 Conflict - Học viên đã được đăng ký vào lớp này
     */
    @PostMapping
    public ResponseEntity<String> createStudentEnrollment(
            @RequestBody @Valid StudentEnrollmentReqDTO.CreateRequest request) {
        log.info("Request create enrollment for student: {} to {} classes",
                request.getStudentId(), request.getScheduleIds().size());

        studentEnrollmentService.createStudentEnrollment(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Đăng ký học viên thành công");
    }

    /**
     * Cập nhật thông tin đăng ký
     * PUT /api/v1/student-enrollments/{enrollmentId}
     * <p>
     * Cập nhật trạng thái enrollment (ACTIVE, RESERVED, TRANSFERRED, DROPPED),
     * ngày nghỉ học và ghi chú. Không cho phép thay đổi học viên hay lớp học.
     *
     * @param enrollmentId ID của enrollment cần cập nhật
     * @param request      Thông tin cập nhật (status, leaveDate, joinDate, note)
     * @return 200 OK - Cập nhật thành công
     * 400 Bad Request - Dữ liệu không hợp lệ
     * 404 Not Found - Không tìm thấy thông tin đăng ký
     */
    @PutMapping("/{enrollmentId}")
    public ResponseEntity<String> updateStudentEnrollment(
            @PathVariable UUID enrollmentId,
            @RequestBody @Valid StudentEnrollmentReqDTO.UpdateRequest request) {
        log.info("Request update enrollment: {} with status: {}", enrollmentId, request.getStatus());

        studentEnrollmentService.updateStudentEnrollment(enrollmentId, request);

        return ResponseEntity.ok("Cập nhật thông tin đăng ký thành công");
    }

    /**
     * Xóa đăng ký học viên
     * DELETE /api/v1/student-enrollments/{enrollmentId}
     * <p>
     * Xóa hoàn toàn thông tin đăng ký học viên khỏi hệ thống.
     * Thao tác này không thể hoàn tác.
     *
     * @param enrollmentId ID của enrollment cần xóa
     * @return 200 OK - Xóa thành công
     * 404 Not Found - Không tìm thấy thông tin đăng ký
     */
    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<String> deleteStudentEnrollment(@PathVariable UUID enrollmentId) {
        log.info("Request delete enrollment: {}", enrollmentId);

        studentEnrollmentService.deleteStudentEnrollment(enrollmentId);

        return ResponseEntity.ok("Xóa đăng ký thành công");
    }

    /**
     * Lấy danh sách lớp học của một học viên (Simple)
     * GET /api/v1/student-enrollments/student/{userId}
     * <p>
     * Trả về danh sách các lớp học mà học viên đang tham gia (trạng thái ACTIVE).
     * Response dạng đơn giản, phù hợp cho dropdown hoặc danh sách tóm tắt.
     *
     * @param studentCode mã định danh công khai của học viên
     * @return 200 OK - Danh sách enrollment
     * 404 Not Found - Không tìm thấy học viên
     */
    @GetMapping("/student/{studentCode}")
    public ResponseEntity<List<StudentEnrollmentResDTO.SimpleResponse>> getStudentEnrollments(
            @PathVariable String studentCode) {
        log.info("Request get enrollments for student: {}", studentCode);

        List<StudentEnrollmentResDTO.SimpleResponse> enrollments =
                studentEnrollmentService.findStudentEnrollmentsByStudentCode(studentCode).stream()
                        .map(studentEnrollmentMapper::toSimpleResponse)
                        .toList();

        return ResponseEntity.ok(enrollments);
    }

    /**
     * Lấy danh sách lớp học của một học viên (Detailed)
     * GET /api/v1/student-enrollments/student/{userId}/detailed
     * <p>
     * Trả về danh sách các lớp học mà học viên đang tham gia (trạng thái ACTIVE)
     * với đầy đủ thông tin chi tiết về học viên và lớp học.
     * Response phù hợp cho trang chi tiết hoặc báo cáo.
     *
     * @param studentCode Mã định danh công khai của học viên
     * @return 200 OK - Danh sách enrollment chi tiết
     * 404 Not Found - Không tìm thấy học viên
     */
    @GetMapping("/student/{studentCode}/detailed")
    public ResponseEntity<List<StudentEnrollmentResDTO.Response>> getDetailedStudentEnrollments(
            @PathVariable String studentCode) {
        log.info("Request get detailed enrollments for student: {}", studentCode);

        List<StudentEnrollmentResDTO.Response> enrollments =
                studentEnrollmentService.findStudentEnrollmentsByStudentCode(studentCode).stream()
                        .map(studentEnrollmentMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(enrollments);
    }

    /**
     * Lấy danh sách học viên trong một lớp học
     * GET /api/v1/student-enrollments/class-schedule/{classScheduleId}
     * <p>
     * Trả về danh sách các học viên đã đăng ký trong một lớp học cụ thể.
     * Response dạng đơn giản, phù hợp cho dropdown hoặc danh sách tóm tắt.
     *
     * @param classScheduleId ID của lớp học
     * @return 200 OK - Danh sách học viên trong lớp
     * 404 Not Found - Không tìm thấy lớp học
     */
    @GetMapping("/class-schedule/{classScheduleId}")
    public ResponseEntity<StudentEnrollmentResDTO.EnrollmentsByScheduleResponse> getStudentEnrollmentsByClassScheduleId(
            @PathVariable String classScheduleId) {
        log.info("Request get enrollments for class schedule: {}", classScheduleId);

        List<StudentEnrollment> enrollments = studentEnrollmentService.getStudentEnrollmentsByClassScheduleId(classScheduleId);

        List<StudentEnrollmentResDTO.EnrolledStudentItem> enrolledStudentItems = studentEnrollmentService.getStudentEnrollmentsByClassScheduleId(classScheduleId).stream()
                .map(studentEnrollmentMapper::toEnrolledStudentItem)
                .toList();

        ClassSchedule schedule = !enrollments.isEmpty() ? enrollments.getFirst().getClassSchedule()
                : classScheduleService.getClassScheduleById(classScheduleId);

        StudentEnrollmentResDTO.EnrollmentsByScheduleResponse response = StudentEnrollmentResDTO.EnrollmentsByScheduleResponse.builder()
                .classScheduleSummary(classScheduleMapper.toClassScheduleSummary(schedule))
                .enrollments(enrolledStudentItems)
                .build();

        return ResponseEntity.ok(response);
    }
}
