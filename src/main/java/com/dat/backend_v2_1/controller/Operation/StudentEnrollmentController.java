package com.dat.backend_v2_1.controller.Operation;

import com.dat.backend_v2_1.dto.Operation.StudentEnrollmentReqDTO;
import com.dat.backend_v2_1.dto.Operation.StudentEnrollmentResDTO;
import com.dat.backend_v2_1.mapper.Operation.StudentEnrollmentMapper;
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

    /**
     * Đăng ký học viên vào lớp học
     * POST /api/v1/student-enrollments
     *
     * Cho phép đăng ký một học viên vào một hoặc nhiều lớp học cùng lúc.
     * Hệ thống sẽ kiểm tra trùng lặp và validate thông tin trước khi tạo enrollment.
     *
     * @param request Thông tin đăng ký (studentId, scheduleIds, joinDate, note)
     * @return 201 Created - Đăng ký thành công
     *         400 Bad Request - Dữ liệu không hợp lệ
     *         404 Not Found - Không tìm thấy học viên hoặc lớp học
     *         409 Conflict - Học viên đã được đăng ký vào lớp này
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
     *
     * Cập nhật trạng thái enrollment (ACTIVE, RESERVED, TRANSFERRED, DROPPED),
     * ngày nghỉ học và ghi chú. Không cho phép thay đổi học viên hay lớp học.
     *
     * @param enrollmentId ID của enrollment cần cập nhật
     * @param request Thông tin cập nhật (status, leaveDate, joinDate, note)
     * @return 200 OK - Cập nhật thành công
     *         400 Bad Request - Dữ liệu không hợp lệ
     *         404 Not Found - Không tìm thấy thông tin đăng ký
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
     *
     * Xóa hoàn toàn thông tin đăng ký học viên khỏi hệ thống.
     * Thao tác này không thể hoàn tác.
     *
     * @param enrollmentId ID của enrollment cần xóa
     * @return 200 OK - Xóa thành công
     *         404 Not Found - Không tìm thấy thông tin đăng ký
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
     *
     * Trả về danh sách các lớp học mà học viên đang tham gia (trạng thái ACTIVE).
     * Response dạng đơn giản, phù hợp cho dropdown hoặc danh sách tóm tắt.
     *
     * @param userId ID của học viên
     * @return 200 OK - Danh sách enrollment
     *         404 Not Found - Không tìm thấy học viên
     */
    @GetMapping("/student/{userId}")
    public ResponseEntity<List<StudentEnrollmentResDTO.SimpleResponse>> getStudentEnrollments(
            @PathVariable UUID userId) {
        log.info("Request get enrollments for student: {}", userId);

        List<StudentEnrollmentResDTO.SimpleResponse> enrollments =
                studentEnrollmentService.findStudentEnrollmentsByUserId(userId);

        return ResponseEntity.ok(enrollments);
    }

    /**
     * Lấy danh sách lớp học của một học viên (Detailed)
     * GET /api/v1/student-enrollments/student/{userId}/detailed
     *
     * Trả về danh sách các lớp học mà học viên đang tham gia (trạng thái ACTIVE)
     * với đầy đủ thông tin chi tiết về học viên và lớp học.
     * Response phù hợp cho trang chi tiết hoặc báo cáo.
     *
     * @param userId ID của học viên
     * @return 200 OK - Danh sách enrollment chi tiết
     *         404 Not Found - Không tìm thấy học viên
     */
    @GetMapping("/student/{userId}/detailed")
    public ResponseEntity<List<StudentEnrollmentResDTO.Response>> getDetailedStudentEnrollments(
            @PathVariable UUID userId) {
        log.info("Request get detailed enrollments for student: {}", userId);

        List<StudentEnrollmentResDTO.Response> enrollments =
                studentEnrollmentService.findDetailedStudentEnrollmentsByUserId(userId);

        return ResponseEntity.ok(enrollments);
    }

    /**
     * Lấy danh sách học viên trong một lớp học
     * GET /api/v1/student-enrollments/class-schedule/{classScheduleId}
     *
     * Trả về danh sách các học viên đã đăng ký trong một lớp học cụ thể.
     * Response dạng đơn giản, phù hợp cho dropdown hoặc danh sách tóm tắt.
     *
     * @param classScheduleId ID của lớp học
     * @return 200 OK - Danh sách học viên trong lớp
     *         404 Not Found - Không tìm thấy lớp học
     */
    @GetMapping("/class-schedule/{classScheduleId}")
    public ResponseEntity<List<StudentEnrollmentResDTO.SimpleResponse>> getStudentEnrollmentsByClassScheduleId(
            @PathVariable String classScheduleId) {
        log.info("Request get enrollments for class schedule: {}", classScheduleId);

        List<StudentEnrollmentResDTO.SimpleResponse> enrollments =
                studentEnrollmentMapper.toSimpleResponseList(studentEnrollmentService.getStudentEnrollmentsByClassScheduleId(classScheduleId));

        return ResponseEntity.ok(enrollments);
    }
}
