package com.dat.ai_receptionist_web.service.Notification;

import com.dat.ai_receptionist_web.domain.Notification.NotificationRecipient;
import com.dat.ai_receptionist_web.dto.Notification.NotificationRecipientDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.NotificationErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Notification.NotificationRecipientMapper;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRecipientRepository;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.enums.Training.NotificationRecipientStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationRecipientService {
    private final NotificationRecipientRepository repository;
    private final NotificationRecipientMapper mapper;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<NotificationRecipientDTO.Response> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationRecipientDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về NotificationRecipientDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public NotificationRecipientDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận NotificationRecipientDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về NotificationRecipientDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public NotificationRecipientDTO.Response create(NotificationRecipientDTO.CreateRequest request) {
        NotificationRecipient entity = new NotificationRecipient();
        entity.setNotification(notificationRepository.findById(request.notificationId()).orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_NOT_FOUND)));
        entity.setRecipientUser(userRepository.findById(request.recipientUserId()).orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND)));
        entity.setRead(request.read());
        entity.setReadAt(request.readAt());
        entity.setDeliveredAt(request.deliveredAt());
        entity.setNotificationRecipientStatus(request.notificationRecipientStatus());
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, NotificationRecipientDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về NotificationRecipientDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public NotificationRecipientDTO.Response update(UUID id, NotificationRecipientDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setNotification(notificationRepository.findById(request.notificationId()).orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_NOT_FOUND)));
        entity.setRecipientUser(userRepository.findById(request.recipientUserId()).orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND)));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setNotificationRecipientStatus(NotificationRecipientStatus.ARCHIVED);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về NotificationRecipient theo kết quả xử lý.
     */
    private NotificationRecipient find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_RECIPIENT_NOT_FOUND));
    }
}


