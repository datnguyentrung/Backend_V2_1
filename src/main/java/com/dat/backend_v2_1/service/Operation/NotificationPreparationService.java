package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.domain.Operation.Notification;
import com.dat.backend_v2_1.domain.Operation.NotificationRecipient;
import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.dto.Operation.AttendanceNotificationPayload;
import com.dat.backend_v2_1.dto.Operation.AttendanceNotificationRow;
import com.dat.backend_v2_1.enums.Operation.AttendanceStatus;
import com.dat.backend_v2_1.enums.Operation.NotificationRecipientStatus;
import com.dat.backend_v2_1.enums.Operation.NotificationType;
import com.dat.backend_v2_1.repository.Operation.NotificationRepository;
import com.dat.backend_v2_1.repository.Operation.StudentAttendanceRepository;
import com.dat.backend_v2_1.repository.Security.AuthTokenRepository;
import com.dat.backend_v2_1.repository.Security.UserProfileRepository;
import com.dat.backend_v2_1.repository.Security.UserRepository;
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
        String scheduleId = row.getScheduleId() == null ? "N/A" : row.getScheduleId();
        String coachName = row.getCoachName() == null ? "He thong" : row.getCoachName();
        String studentName = row.getStudentName() == null ? "Hoc vien" : row.getStudentName();
        LocalDateTime time = row.getCheckInTime() != null ? row.getCheckInTime() : row.getCreatedAt();
        String formattedTime = time == null
                ? ""
                : time.atZone(HO_CHI_MINH_ZONE).format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy", Locale.forLanguageTag("vi-VN")));

        return switch (row.getAttendanceStatus()) {
            case PRESENT -> new NotificationContent(
                    "Diem danh thanh cong",
                    String.format("HV %s da co mat tai lop %s. Luc: %s. HLV: %s", studentName, scheduleId, formattedTime, coachName)
            );
            case LATE -> new NotificationContent(
                    "Thong bao di muon",
                    String.format("He thong ghi nhan HV %s den lop muon tai lop %s. Check-in: %s. GV ghi nhan: %s",
                            studentName, scheduleId, formattedTime, coachName)
            );
            case EXCUSED -> new NotificationContent(
                    "Nghi co phep",
                    String.format("Da xac nhan don xin nghi cua HV %s. Thoi gian: %s. Nguoi duyet: %s",
                            studentName, formattedTime, coachName)
            );
            case MAKEUP -> new NotificationContent(
                    "Diem danh hoc bu",
                    String.format("HV %s duoc ghi nhan hoc bu tai lop %s. Thoi gian: %s. HLV: %s",
                            studentName, scheduleId, formattedTime, coachName)
            );
            default -> new NotificationContent(
                    "Thong bao diem danh",
                    String.format("Cap nhat trang thai diem danh cho HV %s: %s.", studentName, row.getAttendanceStatus())
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
