package com.dat.backend_v2_1.controller.Operation;

import com.dat.backend_v2_1.domain.Operation.CoachAssignment;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentReqDTO;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.dat.backend_v2_1.mapper.Operation.CoachAssignmentMapper;
import com.dat.backend_v2_1.service.Operation.CoachAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý phân công huấn luyện viên (Coach Assignment)
 * Xử lý các thao tác CRUD và tra cứu thông tin phân công HLV
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coach-assignments")
public class CoachAssignmentController {

    private final CoachAssignmentService coachAssignmentService;
    private final CoachAssignmentMapper coachAssignmentMapper;

    /**
     * Phân công huấn luyện viên vào lớp học
     * POST /api/v1/coach-assignments
     * <p>
     * Cho phép phân công một HLV vào một hoặc nhiều lớp học cùng lúc.
     * Hệ thống sẽ kiểm tra trùng lặp và validate thông tin trước khi tạo phân công.
     *
     * @param request Thông tin phân công (coachId, scheduleIds, assignmentDate, endDate, note)
     * @return 201 Created - Phân công thành công
     * 400 Bad Request - Dữ liệu không hợp lệ
     * 404 Not Found - Không tìm thấy HLV hoặc lớp học
     * 409 Conflict - HLV đã được phân công vào lớp này
     */
    @PostMapping
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<List<CoachAssignmentResDTO.SimpleResponse>> createCoachAssignment(
            @RequestBody @Valid CoachAssignmentReqDTO.CreateRequest request) {
        log.info("Request create coach assignment for coach: {} to {} classes",
                request.getCoachId(), request.getScheduleIds().size());

        List<CoachAssignment> coachAssignments = coachAssignmentService.createCoachAssignment(request);

        // Map sang DTO (Đảm bảo bạn đã inject coachAssignmentMapper vào file Service này)
        List<CoachAssignmentResDTO.SimpleResponse> assignmentResponses = coachAssignments.stream()
                .map(coachAssignmentMapper::toSimpleResponse)
                .toList();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentResponses);
    }

    /**
     * Cập nhật thông tin phân công
     * PUT /api/v1/coach-assignments/{coachAssignmentId}
     * <p>
     * Cập nhật trạng thái phân công (ACTIVE, PENDING, COMPLETED, TERMINATED),
     * ngày phân công, ngày kết thúc và ghi chú. Không cho phép thay đổi HLV hay lớp học.
     *
     * @param coachAssignmentId ID của phân công cần cập nhật
     * @param request           Thông tin cập nhật (status, assignmentDate, endDate, note)
     * @return 200 OK - Cập nhật thành công
     * 400 Bad Request - Dữ liệu không hợp lệ
     * 404 Not Found - Không tìm thấy thông tin phân công
     */
    @PutMapping("/{coachAssignmentId}")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<String> updateCoachAssignment(
            @PathVariable UUID coachAssignmentId,
            @RequestBody @Valid CoachAssignmentReqDTO.UpdateRequest request) {
        log.info("Request update coach assignment: {} with status: {}", coachAssignmentId, request.getStatus());

        coachAssignmentService.updateCoachAssignment(coachAssignmentId, request);

        return ResponseEntity.ok("Cập nhật thông tin phân công thành công");
    }

    /**
     * Xóa phân công huấn luyện viên
     * DELETE /api/v1/coach-assignments/{coachAssignmentId}
     * <p>
     * Xóa hoàn toàn thông tin phân công HLV khỏi hệ thống.
     * Thao tác này không thể hoàn tác.
     *
     * @param coachAssignmentId ID của phân công cần xóa
     * @return 200 OK - Xóa thành công
     * 404 Not Found - Không tìm thấy thông tin phân công
     */
    @DeleteMapping("/{coachAssignmentId}")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<Void> deleteCoachAssignment(@PathVariable UUID coachAssignmentId) {
        log.info("Request delete coach assignment: {}", coachAssignmentId);

        coachAssignmentService.deleteCoachAssignment(coachAssignmentId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Lấy danh sách lớp học của một huấn luyện viên
     * GET /api/v1/coach-assignments/coach/{coachId}
     * <p>
     * Trả về danh sách các lớp học mà HLV đang phụ trách (trạng thái ACTIVE).
     * Response dạng đơn giản, phù hợp cho dropdown hoặc danh sách tóm tắt.
     *
     * @param coachId ID của huấn luyện viên
     * @return 200 OK - Danh sách phân công
     * 404 Not Found - Không tìm thấy huấn luyện viên
     */
    @PreAuthorize("@securityRule.isCoach(authentication)")
    @GetMapping("/coach/{coachId}")
    public ResponseEntity<List<CoachAssignmentResDTO.Response>> getCoachAssignments(
            @PathVariable UUID coachId,
            @RequestParam(defaultValue = "ACTIVE") CoachAssignmentStatus status) {
        log.info("Request get assignments for coach: {}", coachId);

        List<CoachAssignmentResDTO.Response> assignments =
                coachAssignmentService.findDetailedCoachAssignmentsByUserId(coachId, status);

        return ResponseEntity.ok(assignments);
    }
}
