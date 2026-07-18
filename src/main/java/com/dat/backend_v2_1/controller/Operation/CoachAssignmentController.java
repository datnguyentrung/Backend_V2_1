package com.dat.backend_v2_1.controller.Operation;

import com.dat.backend_v2_1.domain.Operation.CoachAssignment;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentReqDTO;
import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.dto.PageResponse;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.dat.backend_v2_1.mapper.Operation.CoachAssignmentMapper;
import com.dat.backend_v2_1.service.Operation.CoachAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coach-assignments")
public class CoachAssignmentController {

    private final CoachAssignmentService coachAssignmentService;
    private final CoachAssignmentMapper coachAssignmentMapper;

    @PostMapping
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<List<CoachAssignmentResDTO.SimpleResponse>> createCoachAssignment(
            @RequestBody @Valid CoachAssignmentReqDTO.CreateRequest request) {
        List<CoachAssignment> assignments = coachAssignmentService.createCoachAssignment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignments.stream().map(coachAssignmentMapper::toSimpleResponse).toList());
    }

    @PutMapping("/{coachAssignmentId}")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<String> updateCoachAssignment(
            @PathVariable UUID coachAssignmentId,
            @RequestBody @Valid CoachAssignmentReqDTO.UpdateRequest request) {
        coachAssignmentService.updateCoachAssignment(coachAssignmentId, request);
        return ResponseEntity.ok("Cập nhật thông tin phân công thành công");
    }

    @DeleteMapping("/{coachAssignmentId}")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<Void> deleteCoachAssignment(@PathVariable UUID coachAssignmentId) {
        coachAssignmentService.deleteCoachAssignment(coachAssignmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{coachAssignmentId}")
    @PreAuthorize("@securityRule.isCoach(authentication)")
    public ResponseEntity<CoachAssignmentResDTO.Response> getCoachAssignment(@PathVariable UUID coachAssignmentId) {
        return ResponseEntity.ok(coachAssignmentService.getCoachAssignmentDetail(coachAssignmentId));
    }

    @GetMapping
    @PreAuthorize("@securityRule.isCoach(authentication)")
    public ResponseEntity<PageResponse<CoachAssignmentResDTO.Response>> filterCoachAssignments(
            @RequestParam(required = false) UUID coachId,
            @RequestParam(required = false) String classScheduleId,
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) CoachAssignmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "assignedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(coachAssignmentService.filterCoachAssignments(
                coachId, classScheduleId, branchId, status, startDate, endDate, effectiveDate, search, pageable
        ));
    }

    @GetMapping("/coach/{coachId}")
    @PreAuthorize("@securityRule.isCoach(authentication)")
    public ResponseEntity<List<CoachAssignmentResDTO.Response>> getCoachAssignments(
            @PathVariable UUID coachId,
            @RequestParam(defaultValue = "ACTIVE") CoachAssignmentStatus status) {
        return ResponseEntity.ok(coachAssignmentService.findDetailedCoachAssignmentsByCoachId(coachId, status));
    }

    @GetMapping("/exists")
    @PreAuthorize("@securityRule.isCoach(authentication)")
    public ResponseEntity<Boolean> existsValidAssignment(
            @RequestParam UUID coachId,
            @RequestParam String classScheduleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate) {
        return ResponseEntity.ok(coachAssignmentService.existsValidAssignment(coachId, classScheduleId, workDate));
    }
}
