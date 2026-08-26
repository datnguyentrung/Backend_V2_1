package com.dat.ai_receptionist_web.controller.Notification;

import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Notification.NotificationService;
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
    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_READ.getCode())")
    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<NotificationDTO.Response> theo kết quả xử lý.
     */
    public PageResponse<NotificationDTO.Response> list(Pageable pageable) { return notificationService.list(pageable); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_READ.getCode())")
    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về NotificationDTO.Response theo kết quả xử lý.
     */
    public NotificationDTO.Response get(@PathVariable UUID id) { return notificationService.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_CREATE.getCode())")
    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận NotificationDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về NotificationDTO.Response theo kết quả xử lý.
     */
    public NotificationDTO.Response create(@Valid @RequestBody NotificationDTO.CreateRequest request) { return notificationService.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_UPDATE.getCode())")
    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, NotificationDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về NotificationDTO.Response theo kết quả xử lý.
     */
    public NotificationDTO.Response update(@PathVariable UUID id, @Valid @RequestBody NotificationDTO.UpdateRequest request) { return notificationService.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_DELETE.getCode())")
    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    public void delete(@PathVariable UUID id) { notificationService.delete(id); }
}


