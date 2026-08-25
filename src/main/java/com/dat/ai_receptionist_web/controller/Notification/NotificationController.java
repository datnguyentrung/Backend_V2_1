package com.dat.ai_receptionist_web.controller.Notification;

import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Notification.NotificationCrudService;
import com.dat.ai_receptionist_web.service.Notification.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationCrudService crudService;
    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_READ.getCode())")
    public PageResponse<NotificationDTO.Response> list(Pageable pageable) { return crudService.list(pageable); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_READ.getCode())")
    public NotificationDTO.Response get(@PathVariable UUID id) { return crudService.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_CREATE.getCode())")
    public NotificationDTO.Response create(@Valid @RequestBody NotificationDTO.CreateRequest request) { return notificationService.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_UPDATE.getCode())")
    public NotificationDTO.Response update(@PathVariable UUID id, @Valid @RequestBody NotificationDTO.UpdateRequest request) { return crudService.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_DELETE.getCode())")
    public void delete(@PathVariable UUID id) { crudService.delete(id); }
}
