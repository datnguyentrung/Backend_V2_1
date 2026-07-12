package com.dat.backend_v2_1.controller.Operation;

import com.dat.backend_v2_1.dto.Operation.CoachTimesheetDTO;
import com.dat.backend_v2_1.enums.Operation.CoachTimesheetStatus;
import com.dat.backend_v2_1.service.Operation.CoachTimesheetService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coach-timesheets")
public class CoachTimesheetController {
    private final CoachTimesheetService coachTimesheetService;

    @PostMapping("/check-in")
    @PreAuthorize("@securityRule.isCoach(authentication)")
    public ResponseEntity<CoachTimesheetDTO.Response> checkIn(
            @Valid @RequestBody CoachTimesheetDTO.CheckInRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coachTimesheetService.checkIn(request));
    }

    @GetMapping("/{timesheetId}")
    @PreAuthorize("@securityRule.isCoach(authentication)")
    public ResponseEntity<CoachTimesheetDTO.Response> getDetail(
            @PathVariable UUID timesheetId,
            Authentication authentication) {
        return ResponseEntity.ok(coachTimesheetService.getDetail(timesheetId, authentication));
    }

    @GetMapping
    @PreAuthorize("@securityRule.isCoach(authentication)")
    public ResponseEntity<CoachTimesheetDTO.TimesheetListResponse> filter(
            Authentication authentication,
            @RequestParam(required = false) UUID coachId,
            @RequestParam(required = false) UUID coachAssignmentId,
            @RequestParam(required = false) String classScheduleId,
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) CoachTimesheetStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "workingDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        CoachTimesheetDTO.FilterRequest filter = new CoachTimesheetDTO.FilterRequest(
                coachId, coachAssignmentId, classScheduleId, branchId,
                status, workDate, fromDate, toDate, month, year, search
        );
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(coachTimesheetService.filter(filter, pageable, authentication));
    }

    @GetMapping("/me")
    @PreAuthorize("@securityRule.isCoach(authentication)")
    public ResponseEntity<CoachTimesheetDTO.TimesheetListResponse> myTimesheets(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        CoachTimesheetDTO.FilterRequest filter = new CoachTimesheetDTO.FilterRequest();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setMonth(month);
        filter.setYear(year);
        return ResponseEntity.ok(coachTimesheetService.filter(
                filter,
                PageRequest.of(page, size, Sort.by("workingDate").descending()),
                authentication
        ));
    }

    @PatchMapping("/{timesheetId}")
    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    public ResponseEntity<CoachTimesheetDTO.Response> adjust(
            @PathVariable UUID timesheetId,
            @Valid @RequestBody CoachTimesheetDTO.AdjustRequest request) {
        return ResponseEntity.ok(coachTimesheetService.adjust(timesheetId, request));
    }
}
