package com.dat.backend_v2_1.controller.Operation;

import com.dat.backend_v2_1.dto.Operation.StudentAttendanceDTO;
import com.dat.backend_v2_1.dto.PageResponse;
import com.dat.backend_v2_1.enums.Core.Belt;
import com.dat.backend_v2_1.enums.Core.ScheduleLevel;
import com.dat.backend_v2_1.enums.Operation.AttendanceStatus;
import com.dat.backend_v2_1.enums.Operation.EvaluationStatus;
import com.dat.backend_v2_1.service.Operation.StudentAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student-attendances")
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
     * @param request Thông tin điểm danh (studentId, scheduleId, sessionDate, status, note)
     * @return 201 CREATED + Response DTO chứa đầy đủ thông tin bản ghi vừa tạo
     */
    @PostMapping // URL rõ ràng hành động
    public ResponseEntity<StudentAttendanceDTO.Response> createAttendanceRecordByAdmin(
            @Valid @RequestBody StudentAttendanceDTO.ManualLogRequest request
    ) {
        log.info("Creating attendance record for student {}", request.getStudentId());

        // Service trả về Response DTO để FE hiển thị ngay, không cần gọi GET thêm 1 lần
        StudentAttendanceDTO.Response response = studentAttendanceService.createAttendanceRecord(request);

        // HTTP 201 Created + Return body
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * API Điểm danh nhanh cho học viên (Check-in)
     *
     * @param request Thông tin điểm danh (studentId, scheduleId, sessionDate)
     *                - attendanceStatus mặc định là PRESENT
     *                - checkInTime mặc định là thời điểm hiện tại
     * @return 201 CREATED + Response DTO chứa thông tin bản ghi vừa tạo
     */
    @PostMapping("/check-in") // URL rõ ràng hành động
    public ResponseEntity<StudentAttendanceDTO.Response> createAttendanceRecordByStudent(
            @Valid @RequestBody StudentAttendanceDTO.CreateRequest request
    ) {
        log.info("Creating attendance record for student {}", request.getStudentId());

        StudentAttendanceDTO.Response response = studentAttendanceService.createAttendanceRecord(request);

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
        // 1. Gọi Service xử lý
        List<StudentAttendanceDTO.Response> responses = studentAttendanceService
                .markAsAbsentByScheduleId(request);

        // 2. Trả về 201 Created + Body
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Lọc và lấy danh sách bản ghi điểm danh theo nhiều tiêu chí khác nhau, hỗ trợ phân trang và sắp xếp.
     *
     * @param search             Từ khóa tìm kiếm (tên học viên, email, hoặc ID)
     * @param sessionDate        Ngày của buổi học (yyyy-MM-dd)
     * @param attendanceStatuses Trạng thái điểm danh (PRESENT, ABSENT, EXCUSED, LATE)
     * @param evaluationStatuses Trạng thái đánh giá (GOOD, AVERAGE, POOR)
     * @param belts              Cấp đai của học viên (WHITE, YELLOW, GREEN, BLUE, BROWN, BLACK)
     * @param branchIds          ID chi nhánh (có thể lọc nhiều chi nhánh)
     * @param scheduleLevels     Trình độ của lịch học (BEGINNER, INTERMEDIATE, ADVANCED)
     * @param page               Trang hiện tại (bắt đầu từ 0)
     * @param size               Số bản ghi trên mỗi trang
     * @param sortBy             Trường để sắp xếp (ví dụ: "studentName", "checkInTime")
     * @param sortDir            Hướng sắp xếp ("asc" hoặc "desc")
     * @return 200 OK + Danh sách bản ghi điểm danh
     */
    @GetMapping
    public ResponseEntity<PageResponse<StudentAttendanceDTO.Response>> filterAttendanceRecords(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate sessionDate,
            @RequestParam(required = false) List<AttendanceStatus> attendanceStatuses,
            @RequestParam(required = false) List<EvaluationStatus> evaluationStatuses,
            @RequestParam(required = false) List<Belt> belts,
            @RequestParam(required = false) List<Integer> branchIds,
            @RequestParam(required = false) List<ScheduleLevel> scheduleLevels,

            @RequestParam(required = false) String scheduleId, // Thêm filter theo scheduleId nếu cần thiết

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "sessionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<StudentAttendanceDTO.Response> response = studentAttendanceService
                .getStudentAttendancesWithStats(
                        search, sessionDate, attendanceStatuses, evaluationStatuses,
                        belts, branchIds, scheduleLevels, scheduleId, pageable
                );

        return ResponseEntity.ok(response);
    }
}
