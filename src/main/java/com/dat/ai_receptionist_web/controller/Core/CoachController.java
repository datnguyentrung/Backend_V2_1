package com.dat.ai_receptionist_web.controller.Core;

import com.dat.ai_receptionist_web.dto.Core.CoachReqDTO;
import com.dat.ai_receptionist_web.dto.Core.CoachResDTO;
import com.dat.ai_receptionist_web.service.Core.CoachService;
import com.dat.ai_receptionist_web.service.Operation.CoachAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coaches")
public class CoachController {

    private final CoachService coachService;

    /**
     * Tạo huấn luyện viên mới
     * POST /api/v1/coaches
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<CoachResDTO.CoachDetail> createCoachMultipart(
            @RequestPart("data") @Valid CoachReqDTO.CoachCreate createDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Request create coach with face image: {}", createDTO.getFullName());
        return ResponseEntity.status(HttpStatus.CREATED).body(coachService.createCoach(createDTO, file));
    }

    /**
     * Lấy thông tin chi tiết huấn luyện viên
     * GET /api/v1/coaches/{staffCode}
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
     * PUT /api/v1/coaches/{personId}
     */
    @PutMapping(value = "/{personId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<CoachResDTO.CoachDetail> updateCoachMultipart(
            @PathVariable UUID personId,
            @RequestPart("data") @Valid CoachReqDTO.CoachUpdate updateDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Request update coach with face image: {}", personId);
        updateDTO.setPersonId(personId);
        return ResponseEntity.ok(coachService.updateCoach(updateDTO, file));
    }

    /**
     * Xóa huấn luyện viên (Soft Delete)
     * DELETE /api/v1/coaches/{personId}
     */
    @DeleteMapping("/{personId}")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<Void> deleteCoach(@PathVariable UUID personId) {
        log.info("Request delete coach: {}", personId);

        coachService.deleteCoach(personId);

        return ResponseEntity.ok().build();
    }

    /**
     * Xóa vĩnh viễn huấn luyện viên (Hard Delete) - ADMIN ONLY
     * DELETE /api/v1/coaches/{staffCode}/permanent
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

