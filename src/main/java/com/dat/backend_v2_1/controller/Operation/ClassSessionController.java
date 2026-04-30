package com.dat.backend_v2_1.controller.Operation;

import com.dat.backend_v2_1.dto.Operation.ClassSessionDTO;
import com.dat.backend_v2_1.dto.PageResponse;
import com.dat.backend_v2_1.service.Operation.ClassSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/class-sessions")
public class ClassSessionController {
    private final ClassSessionService classSessionService;

    @GetMapping
    @PreAuthorize("@securityRule.isCoach(authentication)")
    public ResponseEntity<PageResponse<ClassSessionDTO.SessionResponse>> filterClassSessions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate sessionDate,
            @RequestParam(required = false) Boolean isAttendanceClosed,

            @RequestParam(required = false) List<String> scheduleIds,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "sessionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        log.info("Request lọc buổi học với search: {}, sessionDate: {}, isAttendanceClosed: {}, scheduleIds: {}, page: {}, size: {}, sortBy: {}, sortDir: {}",
                search, sessionDate, isAttendanceClosed, scheduleIds, page, size, sortBy, sortDir);

        // 1. Sắp xếp chính (theo tham số sortBy truyền vào, mặc định là sessionDate DESC)
        Sort primarySort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // 2. Sắp xếp phụ (Luôn luôn phụ thêm: nếu ngày trùng nhau thì xếp theo scheduleId ASC)
        // Lưu ý: Phải dùng "classSchedule.scheduleId" vì nó là thuộc tính lồng trong Entity ClassSession
        Sort finalSort = primarySort.and(Sort.by("classSchedule.scheduleId").ascending());

        Pageable pageable = PageRequest.of(page, size, finalSort);

        PageResponse<ClassSessionDTO.SessionResponse> response = classSessionService.filterClassSessions(
                search, sessionDate, isAttendanceClosed, scheduleIds, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ClassSessionDTO.SessionResponse> getClassSessionById(@PathVariable UUID sessionId) {
        log.info("Request lấy thông tin buổi học ID: {}", sessionId);
        ClassSessionDTO.SessionResponse response = classSessionService.getClassSessionById(sessionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ClassSessionDTO.SessionResponse> createClassSession(@RequestBody ClassSessionDTO.SessionCreateRequest request) {
        log.info("Request tạo buổi học mới cho lịch học ID: {}", request.getScheduleId());
        ClassSessionDTO.SessionResponse response = classSessionService.createClassSession(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<ClassSessionDTO.SessionResponse> updateClassSession(
            @PathVariable UUID sessionId,
            @RequestBody ClassSessionDTO.SessionUpdateRequest request) {
        log.info("Request cập nhật buổi học ID: {}", sessionId);
        ClassSessionDTO.SessionResponse response = classSessionService.updateClassSession(sessionId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteClassSession(@PathVariable UUID sessionId) {
        log.info("Request xóa buổi học ID: {}", sessionId);
        classSessionService.deleteClassSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
