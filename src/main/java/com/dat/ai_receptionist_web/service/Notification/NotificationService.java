package com.dat.ai_receptionist_web.service.Notification;
import com.dat.ai_receptionist_web.domain.Notification.*;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Training.NotificationRecipientStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.NotificationErrorCode;
import com.dat.ai_receptionist_web.mapper.Notification.NotificationMapper;
import com.dat.ai_receptionist_web.repository.Notification.*;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final UserRepository userRepository;
    private final UserPersonRepository userPersonRepository;
    private final NotificationDeliveryService deliveryService;
    private final NotificationMapper notificationMapper;
    private final TransactionAfterCommitExecutor afterCommitExecutor;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<NotificationDTO.Response> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationDTO.Response> list(Pageable pageable) {
        return PageResponse.of(notificationRepository.findAll(pageable), notificationMapper::toResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về NotificationDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public NotificationDTO.Response get(UUID id) {
        return notificationMapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận NotificationDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về NotificationDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public NotificationDTO.Response create(NotificationDTO.CreateRequest request) {
        Set<UUID> recipientIds = resolveRecipients(request);
        if (recipientIds.isEmpty()) {
            throw new ApiException(NotificationErrorCode.NOTIFICATION_RECIPIENT_REQUIRED);
        }
        List<User> users = userRepository.findAllById(recipientIds);
        if (users.size() != recipientIds.size())
            throw new ApiException(NotificationErrorCode.NOTIFICATION_RECIPIENTS_NOT_FOUND);
        Notification notification = notificationRepository.save(Notification.builder()
                .title(request.title()).body(request.body()).notificationType(request.type())
                .referenceType(request.referenceType()).referenceId(request.referenceId())
                .payload(request.payload()).build());
        users.forEach(user -> recipientRepository.save(NotificationRecipient.builder()
                .notification(notification).recipientUser(user).read(false)
                .notificationRecipientStatus(NotificationRecipientStatus.PENDING).build()));
        afterCommitExecutor.afterCommit(() ->
                deliveryService.deliver(notification.getNotificationId()));
        return new NotificationDTO.Response(notification.getNotificationId(), users.size());
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, NotificationDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về NotificationDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public NotificationDTO.Response update(UUID id, NotificationDTO.UpdateRequest request) {
        Notification notification = find(id);
        notificationMapper.updateEntity(request, notification);
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(UUID id) {
        notificationRepository.delete(find(id));
    }

    /**
     * Tác dụng: Thực hiện logic resolveRecipients của lớp hiện tại.
     * Input: Nhận NotificationDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về Set<UUID> theo kết quả xử lý.
     */
    private Set<UUID> resolveRecipients(NotificationDTO.CreateRequest request) {
        Set<UUID> recipients = new HashSet<>(safe(request.recipientUserIds()));
        safe(request.recipientPersonIds()).forEach(personId ->
                recipients.addAll(userPersonRepository.findActiveUserIdsByPersonId(personId)));
        safe(request.recipientRoleCodes()).stream()
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .forEach(roleCode -> recipients.addAll(userRepository.findUserIdsByRoleCode(roleCode)));
        return recipients;
    }

    /**
     * Tác dụng: Thực hiện logic safe của lớp hiện tại.
     * Input: Nhận Set<T> values từ caller hoặc request.
     * Output: Trả về Set<T> theo kết quả xử lý.
     */
    private <T> Set<T> safe(Set<T> values) {
        return values == null ? Set.of() : values;
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về Notification theo kết quả xử lý.
     */
    private Notification find(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }
}


