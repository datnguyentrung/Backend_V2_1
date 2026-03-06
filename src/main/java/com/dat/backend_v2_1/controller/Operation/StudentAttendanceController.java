package com.dat.backend_v2_1.controller.Operation;

import com.dat.backend_v2_1.dto.Operation.StudentAttendanceDTO;
import com.dat.backend_v2_1.enums.Security.UserStatus;
import com.dat.backend_v2_1.service.Operation.StudentAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student-attendance")
public class StudentAttendanceController {
    private final StudentAttendanceService studentAttendanceService;

    @PatchMapping("/{attendanceId}/status")
    public ResponseEntity<Void> updateAttendanceStatus(
            Authentication authentication, // Giả sử dùng Spring Security
            @PathVariable UUID attendanceId,
            @RequestBody @Valid StudentAttendanceDTO.UpdateStatusRequest request
    ) {
        String coachId = authentication.getName();

        log.info("Coach {} updating attendance {} to status {}",
                 coachId, attendanceId, request.getAttendanceStatus());

        studentAttendanceService.updateAttendanceStatus(coachId, request, attendanceId);

        // Cách 1: Trả về 204 No Content (Chuẩn REST khi không trả về dữ liệu)
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{attendanceId}/evaluation")
    public ResponseEntity<Void> updateAttendanceEvaluation(
            Authentication authentication,
            @PathVariable UUID attendanceId,
            @RequestBody @Valid StudentAttendanceDTO.UpdateEvaluationRequest request
    ) {
        String coachId = authentication.getName();

        log.info("Coach {} updating attendance {} to evaluation {}",
                 coachId, attendanceId, request.getEvaluationStatus());

        studentAttendanceService.updateAttendanceEvaluation(coachId, request, attendanceId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Tạo bản ghi điểm danh thủ công cho 1 học viên
     *
     * @param authentication JWT authentication chứa coachId
     * @param request Thông tin điểm danh (studentId, scheduleId, sessionDate, status, note)
     * @return 201 CREATED + Response DTO chứa đầy đủ thông tin bản ghi vừa tạo
     */
    @PostMapping
    public ResponseEntity<StudentAttendanceDTO.Response> createAttendanceRecord(
            Authentication authentication,
            @Valid @RequestBody StudentAttendanceDTO.ManualLogRequest request
    ) {
        String coachId = authentication.getName();

        log.info("Coach {} creating attendance record for student {} on {}",
                 coachId, request.getStudentId(), request.getSessionDate());

        // Service trả về Response DTO để FE hiển thị ngay, không cần gọi GET thêm 1 lần
        StudentAttendanceDTO.Response response = studentAttendanceService.createAttendanceRecord(request, coachId);

        // HTTP 201 Created + Return body
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * API Khởi tạo danh sách điểm danh cho buổi học.
     * <p>
     * Logic:
     * - Nếu chưa có dữ liệu: Tạo mới toàn bộ với trạng thái ABSENT.
     * - Nếu đã có dữ liệu (1 phần): Chỉ tạo thêm những người thiếu, giữ nguyên người cũ.
     * - Trả về: Full danh sách để hiển thị ngay lập tức.
     */
    @PostMapping("/batch-init") // URL rõ ràng hành động
    public ResponseEntity<List<StudentAttendanceDTO.Response>> initializeAttendance(
            @AuthenticationPrincipal Jwt jwt, // Best practice: Lấy token đã decode
            @Valid @RequestBody StudentAttendanceDTO.BatchCreateRequest request
    ) {
        // 1. Lấy Coach ID từ Token (Subject thường là UserID/CoachID)
        String coachId = jwt.getSubject();

        // (Optional) Log request để dễ trace lỗi production
        log.info("Rest Request to initialize attendance for Schedule: {} by Coach: {}",
                request.getClassScheduleId(), coachId);

        // 2. Gọi Service xử lý
        List<StudentAttendanceDTO.Response> responses = studentAttendanceService
                .markAsAbsentByScheduleId(request, coachId);

        // 3. Trả về 201 Created + Body
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Lọc và lấy danh sách bản ghi điểm danh theo lịch học và ngày buổi học
     *
     * @param jwt JWT chứa thông tin user và status
     * @param classScheduleId ID lịch học cần lọc
     * @param sessionDate Ngày buổi học cần lọc
     * @return 200 OK + Danh sách bản ghi điểm danh
    */
    @GetMapping("/filter")
    public ResponseEntity<List<StudentAttendanceDTO.Response>> filterAttendanceRecords(
            @AuthenticationPrincipal Jwt jwt, // Inject thẳng Jwt vào đây
            @RequestParam String classScheduleId,
            @RequestParam LocalDate sessionDate
    ) {
        // --- BƯỚC 1: CHECK QUYỀN (AUTHORIZATION) ---
        Map<String, Object> userClaim = jwt.getClaim("user");

        // Validate null an toàn hơn
        if (userClaim == null || !userClaim.containsKey("status")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid Token: Missing user status");
        }

        String statusString = (String) userClaim.get("status");

        try {
            UserStatus status = UserStatus.valueOf(statusString);

            // Nếu KHÔNG PHẢI ACTIVE -> Ném lỗi ngay lập tức
            if (status != UserStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Tài khoản của bạn chưa được kích hoạt (Status: " + status + ")");
            }
        } catch (IllegalArgumentException e) {
            // Phòng trường hợp Token chứa status lạ không có trong Enum
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid User Status in Token");
        }

        // --- BƯỚC 2: XỬ LÝ NGHIỆP VỤ (HAPPY PATH) ---
        // Code chạy đến đây nghĩa là User chắc chắn đã ACTIVE

        List<StudentAttendanceDTO.Response> responses = studentAttendanceService
                .filterAttendanceRecords(classScheduleId, sessionDate);

        return ResponseEntity.ok(responses);
    }
}
