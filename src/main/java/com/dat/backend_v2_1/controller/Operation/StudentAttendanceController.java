package com.dat.backend_v2_1.controller.Operation;

import com.dat.backend_v2_1.config.SecurityRule;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.dto.Operation.StudentAttendanceDTO;
import com.dat.backend_v2_1.dto.Operation.StudentEnrollmentResDTO;
import com.dat.backend_v2_1.enums.Core.Belt;
import com.dat.backend_v2_1.enums.Core.ScheduleLevel;
import com.dat.backend_v2_1.enums.Operation.AttendanceStatus;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.dat.backend_v2_1.enums.Operation.EvaluationStatus;
import com.dat.backend_v2_1.service.Operation.CoachAssignmentService;
import com.dat.backend_v2_1.service.Operation.StudentAttendanceService;
import com.dat.backend_v2_1.service.Operation.TuitionPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student-attendances")
public class StudentAttendanceController {
    private final StudentAttendanceService studentAttendanceService;
    private final TuitionPaymentService tuitionPaymentService;
    private final SecurityRule securityRule;
    private final CoachAssignmentService coachAssignmentService;

    /*
     * API Cập nhật điểm danh hàng loạt cho nhiều học viên trong cùng 1 buổi học.
     * Logic:
     * - Nhận vào List<StudentAttendanceDTO.SimpleResponse> chứa attendanceId và các trường cần cập nhật (status, note, evaluationStatus).
     * - Duyệt qua từng item, kiểm tra quyền hạn (chỉ Coach phụ trách lớp đó mới được cập nhật).
     * - Cập nhật từng bản ghi tương ứng trong database.
     * - Trả về List<StudentAttendanceDTO.Response> mới nhất sau khi cập nhật để Frontend đồng bộ UI ngay lập tức.
     * Lưu ý: Không nên trả về 204 No Content vì FE cần dữ liệu mới nhất để hiển thị, tránh phải gọi GET thêm 1 lần nữa.
     */
    @PreAuthorize("@securityRule.isCoach(authentication)")
    @PutMapping
    public ResponseEntity<List<StudentAttendanceDTO.Response>> updateAttendanceRecords(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid List<StudentAttendanceDTO.SimpleResponse> request
    ) {
        // Chỉ nên log size của request, log toàn bộ data sẽ làm phình file log rất nhanh
        log.info("updateAttendanceRecords called by coach: {} for {} records", jwt.getSubject(), request.size());

        String coachId = jwt.getSubject();

        // Gọi thẳng vào Service, truyền ID
        List<StudentAttendanceDTO.Response> updatedData = studentAttendanceService.updateStudentAttendance(request, coachId);

        // Trả về 200 OK kèm dữ liệu mới nhất để Frontend đồng bộ UI
        return ResponseEntity.ok(updatedData);
    }

    @PreAuthorize("@securityRule.isCoach(authentication)")
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

    @PreAuthorize("@securityRule.isCoach(authentication)")
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
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
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
     * @return 201 CREATED + Response DTO chứa thông tin bản ghi vừa tạo
     */
//    @PreAuthorize("@securityRule.isCoach(authentication)")
    @PostMapping("/check-in")
    public ResponseEntity<?> createAttendanceRecordByStudent(
            // Đổi thành <?> để có thể trả về cả DTO hoặc Message lỗi
            @Valid @RequestBody StudentAttendanceDTO.CreateRequest request
    ) {
        log.info("Creating attendance record for student {}", request.getStudentId());

        try {
            StudentAttendanceDTO.Response response = studentAttendanceService.createAttendanceRecord(request);

            if (response == null) {
                // Trường hợp 1: Có ca học nhưng đã điểm danh -> Báo Conflict 409
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            // Điểm danh thành công -> Báo Created 201
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (NoSuchElementException e) {
            // Trường hợp 2: Không có ca học -> Báo Not Found 404
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (IllegalStateException e) {
            // Trường hợp học viên không ACTIVE -> Báo Bad Request 400
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
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
    @PreAuthorize("@securityRule.isCoach(authentication)")
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
    @PreAuthorize("@securityRule.isCoach(authentication) || @securityRule.isParent(authentication) || @securityRule.isStudent(authentication)")
    @GetMapping
    public ResponseEntity<StudentAttendanceDTO.AttendanceListResponse> filterAttendanceRecords(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate sessionDate,
            @RequestParam(required = false) List<AttendanceStatus> attendanceStatuses,
            @RequestParam(required = false) List<EvaluationStatus> evaluationStatuses,
            @RequestParam(required = false) List<Belt> belts,
            @RequestParam(required = false) List<Integer> branchIds,
            @RequestParam(required = false) List<ScheduleLevel> scheduleLevels,

            @RequestParam(required = false) List<String> scheduleIds, // Thêm filter theo scheduleId nếu cần thiết
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "sessionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        List<StudentEnrollmentResDTO.EnrollmentHistoryItem> items = null;

        // 2. Xử lý scheduleIds từ tham số truyền vào
        if (scheduleIds != null && !scheduleIds.isEmpty()) {
            items = scheduleIds.stream()
                    .map(id -> StudentEnrollmentResDTO.EnrollmentHistoryItem.builder()
                            .scheduleId(id)
                            .build())
                    .collect(Collectors.toList()); // Tạo list mới gán vào items
        }

        if (securityRule.isHeadCoach(authentication)) {
            // Xử lý riêng nếu là HEAD_COACH / ADMIN
            // VD: Lấy full quyền hạn, toàn bộ dữ liệu hệ thống

        } else if (securityRule.isManagerSenior(authentication)) {
            // Xử lý riêng nếu là MANAGER
            // VD: Lấy danh sách các cơ sở do Manager này quản lý

        } else if (securityRule.isCoach(authentication)) {
            if (scheduleIds == null || scheduleIds.isEmpty()) {
                // Nếu không có filter theo scheduleId, lấy danh sách các lớp do Coach này phụ trách
                List<CoachAssignmentResDTO.SimpleResponse> coachAssignments =
                        coachAssignmentService.findCoachAssignmentsByCoachId(UUID.fromString(authentication.getName()), CoachAssignmentStatus.ACTIVE);

                // Tạo list từ assignments của Coach
                items = coachAssignments.stream()
                        .map(assignment -> StudentEnrollmentResDTO.EnrollmentHistoryItem.builder()
                                .scheduleId(assignment.getClassSchedule().getScheduleId())
                                .joinDate(assignment.getAssignedDate())
                                .leaveDate(assignment.getEndDate())
                                .build())
                        .collect(Collectors.toList());
            }
        } else {
            // Xử lý cho các Role còn lại (như STUDENT)
            // Không làm gì thêm, chỉ trả về thông tin UserRes cơ bản đã map ở trên
        }

        StudentAttendanceDTO.AttendanceListResponse response = studentAttendanceService
                .getStudentAttendancesWithStats(
                        pageable,
                        search, sessionDate, attendanceStatuses, evaluationStatuses,
                        belts, branchIds, scheduleLevels, items,
                        startDate, endDate
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<Void> deleteAttendanceRecords(@RequestBody List<UUID> attendanceIds) {
        log.info("Deleting attendance records: {}", attendanceIds);
        studentAttendanceService.deleteAttendanceRecords(attendanceIds);

        return ResponseEntity.noContent().build(); // Sửa chỗ này
    }
}
