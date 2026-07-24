package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.domain.Operation.Notification;
import com.dat.ai_receptionist_web.domain.Operation.NotificationRecipient;
import com.dat.ai_receptionist_web.dto.Operation.AttendanceNotificationPayload;
import com.dat.ai_receptionist_web.dto.Operation.AttendanceNotificationRow;
import com.dat.ai_receptionist_web.enums.Operation.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Operation.NotificationRecipientStatus;
import com.dat.ai_receptionist_web.enums.Operation.NotificationType;
import com.dat.ai_receptionist_web.repository.Operation.NotificationRepository;
import com.dat.ai_receptionist_web.repository.Operation.StudentAttendanceRepository;
import com.dat.ai_receptionist_web.repository.Security.AuthTokenRepository;
import com.dat.ai_receptionist_web.repository.Security.UserProfileRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreparationService {

    private static final ZoneId HO_CHI_MINH_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final StudentAttendanceRepository studentAttendanceRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthTokenRepository authTokenRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AttendanceNotificationPayload> preparePendingNotification(UUID attendanceId) {
        AttendanceNotificationRow row = studentAttendanceRepository.findAttendanceNotificationRow(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("Attendance not found: " + attendanceId));

        if (row.getAttendanceStatus() == AttendanceStatus.ABSENT) {
            log.debug("Skipping attendance notification for absent attendanceId={}", attendanceId);
            return Optional.empty();
        }

        List<UUID> recipientUserIds = userProfileRepository.findActiveUserIdsByPersonId(row.getStudentPersonId())
                .stream()
                .distinct()
                .toList();
        if (recipientUserIds.isEmpty()) {
            log.info("Skipping attendance notification for attendanceId={} because no active recipient user exists", attendanceId);
            return Optional.empty();
        }

        List<String> tokens = authTokenRepository.findActiveFcmTokens(row.getStudentPersonId())
                .stream()
                .filter(token -> token != null && !token.isBlank())
                .distinct()
                .toList();
        if (tokens.isEmpty()) {
            log.info("Skipping attendance notification for attendanceId={} because no active FCM token exists", attendanceId);
            return Optional.empty();
        }

        NotificationContent content = buildContent(row);
        Map<String, String> data = new HashMap<>();
        data.put("screen", "AttendanceHistory");
        data.put("personId", row.getStudentPersonId().toString());
        data.put("attendanceId", row.getAttendanceId().toString());

        Notification notification = Notification.builder()
                .title(content.title())
                .body(content.body())
                .notificationType(NotificationType.ATTENDANCE)
                .referenceType("STUDENT_ATTENDANCE")
                .referenceId(row.getAttendanceId().toString())
                .payload(toJson(data))
                .build();

        notification.getRecipients().addAll(recipientUserIds.stream()
                .map(userId -> NotificationRecipient.builder()
                        .notification(notification)
                        .recipientUser(userRepository.getReferenceById(userId))
                        .recipientStatus(NotificationRecipientStatus.PENDING)
                        .build())
                .toList());

        Notification savedNotification = notificationRepository.save(notification);
        return Optional.of(new AttendanceNotificationPayload(
                savedNotification.getNotificationId(),
                tokens,
                content.title(),
                content.body(),
                Map.copyOf(data)
        ));
    }

    private NotificationContent buildContent(AttendanceNotificationRow row) {
        String scheduleId = row.getScheduleId() == null ? "chưa cập nhật" : row.getScheduleId();
        String coachName = row.getCoachName() == null ? "chưa cập nhật" : row.getCoachName();
        String studentName = row.getStudentName() == null ? "học viên" : row.getStudentName();
        LocalDateTime time = row.getCheckInTime() != null ? row.getCheckInTime() : row.getCreatedAt();
        String formattedTime = time == null
                ? "chưa cập nhật"
                : time.atZone(HO_CHI_MINH_ZONE).format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy", Locale.forLanguageTag("vi-VN")));

        return switch (row.getAttendanceStatus()) {
            case PRESENT -> new NotificationContent(
                    "Thông báo điểm danh",
                    String.format("""
                            Học viên %s đã có mặt tại lớp.
                            Lớp: %s
                            Thời gian: %s
                            Huấn luyện viên: %s""", studentName, scheduleId, formattedTime, coachName)
            );
            case LATE -> new NotificationContent(
                    "Thông báo điểm danh",
                    String.format("""
                            Học viên %s được ghi nhận đến lớp muộn.
                            Lớp: %s
                            Thời gian check-in: %s
                            Người ghi nhận: %s""",
                            studentName, scheduleId, formattedTime, coachName)
            );
            case EXCUSED -> new NotificationContent(
                    "Thông báo điểm danh",
                    String.format("""
                            Đơn xin nghỉ của học viên %s đã được xác nhận.
                            Thời gian: %s
                            Người duyệt: %s""",
                            studentName, formattedTime, coachName)
            );
            case MAKEUP -> new NotificationContent(
                    "Thông báo điểm danh",
                    String.format("""
                            Học viên %s đã được ghi nhận học bù.
                            Lớp: %s
                            Thời gian: %s
                            Huấn luyện viên: %s""",
                            studentName, scheduleId, formattedTime, coachName)
            );
            default -> new NotificationContent(
                    "Thông báo điểm danh",
                    String.format("""
                            Trạng thái điểm danh của học viên %s đã được cập nhật.
                            Trạng thái: %s""", studentName, row.getAttendanceStatus())
            );
        };
    }

    private String toJson(Map<String, String> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Notification payload is invalid", e);
        }
    }

    private record NotificationContent(String title, String body) {
    }
}
