package com.dat.backend_v2_1.controller.Operation;

import com.dat.backend_v2_1.dto.Operation.NotificationDTO;
import com.dat.backend_v2_1.enums.Operation.NotificationRecipientStatus;
import com.dat.backend_v2_1.enums.Operation.NotificationType;
import com.dat.backend_v2_1.service.Operation.NotificationRecipientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification-recipients")
public class NotificationRecipientController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "readAt",
            "deliveredAt",
            "updatedAt",
            "recipientStatus",
            "read"
    );

    private final NotificationRecipientService notificationRecipientService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationDTO.RecipientListResponse> getMine(
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) NotificationRecipientStatus status,
            @RequestParam(name = "type", required = false) NotificationType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromCreatedAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toCreatedAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromReadAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toReadAt,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String resolvedSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(resolvedSortBy).ascending()
                : Sort.by(resolvedSortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), pageSize, sort);
        return ResponseEntity.ok(notificationRecipientService.filterForCurrentUser(
                read,
                status,
                type,
                fromCreatedAt,
                toCreatedAt,
                fromReadAt,
                toReadAt,
                search,
                pageable
        ));
    }

    @GetMapping("/{notificationRecipientId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationDTO.RecipientResponse> getDetail(@PathVariable UUID notificationRecipientId) {
        return ResponseEntity.ok(notificationRecipientService.getMine(notificationRecipientId));
    }

    @PatchMapping("/{notificationRecipientId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markRead(@PathVariable UUID notificationRecipientId) {
        notificationRecipientService.markRead(notificationRecipientId);
        return ResponseEntity.noContent().build();
    }
}
