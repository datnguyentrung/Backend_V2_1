package com.dat.backend_v2_1.controller.Core;

import com.dat.backend_v2_1.dto.Core.CoachReqDTO;
import com.dat.backend_v2_1.dto.Core.CoachResDTO;
import com.dat.backend_v2_1.service.Core.CoachService;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coaches")
public class CoachController {

    private final CoachService coachService;
    private final CoachAssignmentService coachAssignmentService;

    /**
     * Tạo huấn luyện viên mới
     * POST /api/v1/coaches
     */
    @PostMapping
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<CoachResDTO.CoachDetail> createCoach(
            @RequestBody @Valid CoachReqDTO.CoachCreate createDTO) {
        log.info("Request create coach: {}", createDTO.getFullName());

        CoachResDTO.CoachDetail newCoach = coachService.createCoach(createDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(newCoach);
    }

    /**
     * Lấy thông tin chi tiết huấn luyện viên
     * GET /api/v1/coaches/{userId}
     */
    @PreAuthorize("@securityRule.isCoach(authentication)")
    @GetMapping("/{staffCode}")
    public ResponseEntity<CoachResDTO.CoachDetail> getCoachDetail(
            @PathVariable String staffCode) {
        log.info("Request get coach detail: {}", staffCode);

        CoachResDTO.CoachDetail coachDetail = coachService.getCoachDetail(staffCode);

        return ResponseEntity.ok(coachDetail);
    }

    /**
     * Lấy danh sách tất cả huấn luyện viên
     * GET /api/v1/coaches
     */
    @PreAuthorize("@securityRule.isCoach(authentication)")
    @GetMapping
    public ResponseEntity<List<CoachResDTO.CoachDetail>> getAllCoaches() {
        log.info("Request get all coaches");

        List<CoachResDTO.CoachDetail> coaches = coachService.getAllCoaches();

        return ResponseEntity.ok(coaches);
    }

    /**
     * Cập nhật thông tin huấn luyện viên
     * PUT /api/v1/coaches/{userId}
     */
    @PutMapping("/{userId}")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<CoachResDTO.CoachDetail> updateCoach(
            @PathVariable UUID userId,
            @RequestBody @Valid CoachReqDTO.CoachUpdate updateDTO) {
        log.info("Request update coach: {}", userId);

        // Set userId từ path variable
        updateDTO.setUserId(userId);

        CoachResDTO.CoachDetail updatedCoach = coachService.updateCoach(updateDTO);

        return ResponseEntity.ok(updatedCoach);
    }

    /**
     * Xóa huấn luyện viên (Soft Delete)
     * DELETE /api/v1/coaches/{userId}
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<Void> deleteCoach(@PathVariable UUID userId) {
        log.info("Request delete coach: {}", userId);

        coachService.deleteCoach(userId);

        return ResponseEntity.ok().build();
    }

    /**
     * Xóa vĩnh viễn huấn luyện viên (Hard Delete) - ADMIN ONLY
     * DELETE /api/v1/coaches/{userId}/permanent
     * ⚠️ CẢNH BÁO: Không thể hoàn tác!
     */
    @DeleteMapping("/{staffCode}/permanent")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<Void> permanentlyDeleteCoach(@PathVariable String staffCode) {
        log.warn("⚠️ Request PERMANENTLY delete coach: {}", staffCode);

        coachService.permanentlyDeleteCoach(staffCode);

        return ResponseEntity.ok().build();
    }
}
