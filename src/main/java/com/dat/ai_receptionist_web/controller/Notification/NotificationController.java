package com.dat.ai_receptionist_web.controller.Notification;
import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import com.dat.ai_receptionist_web.service.Notification.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_CREATE.getCode())")
    public NotificationDTO.Response create(@Valid @RequestBody NotificationDTO.CreateRequest request) {
        return notificationService.create(request);
    }
}
