package com.dat.ai_receptionist_web.controller.Notification;

import com.dat.ai_receptionist_web.dto.Notification.NotificationRecipientDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Notification.NotificationRecipientCrudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification-recipients")
@RequiredArgsConstructor
public class NotificationRecipientController {
    private final NotificationRecipientCrudService service;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_RECIPIENT_READ.getCode())")
    public PageResponse<NotificationRecipientDTO.Response> list(Pageable pageable) { return service.list(pageable); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_RECIPIENT_READ.getCode())")
    public NotificationRecipientDTO.Response get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_RECIPIENT_CREATE.getCode())")
    public NotificationRecipientDTO.Response create(@Valid @RequestBody NotificationRecipientDTO.CreateRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_RECIPIENT_UPDATE.getCode())")
    public NotificationRecipientDTO.Response update(@PathVariable UUID id, @Valid @RequestBody NotificationRecipientDTO.UpdateRequest request) { return service.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_RECIPIENT_DELETE.getCode())")
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
