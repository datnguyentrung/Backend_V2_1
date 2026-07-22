package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.config.RabbitMQConfig;
import com.dat.backend_v2_1.dto.Operation.AttendanceNotificationPayload;
import com.dat.backend_v2_1.dto.Operation.ClassSessionCompletedNotificationMessage;
import com.dat.backend_v2_1.dto.Operation.ClassSessionReportPayload;
import com.dat.backend_v2_1.dto.Operation.CompletedSessionAttendanceNotificationRow;
import com.dat.backend_v2_1.dto.Operation.FirebaseMulticastResult;
import com.dat.backend_v2_1.dto.Operation.NotificationDTO;
import com.dat.backend_v2_1.repository.Operation.StudentAttendanceRepository;
import com.dat.backend_v2_1.repository.Security.AuthTokenRepository;
import com.dat.backend_v2_1.repository.Security.UserProfileRepository;
import com.dat.backend_v2_1.repository.Security.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassSessionNotificationService {

    private static final String HEAD_COACH_ROLE_CODE = "HEAD_COACH";
    private static final ZoneId HO_CHI_MINH_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));

    private final ClassSessionReportService classSessionReportService;
    private final NotificationService notificationService;
    private final FirebaseNotificationSender firebaseNotificationSender;
    private final NotificationStatusService notificationStatusService;
    private final AttendanceNotificationTaskExecutor notificationTaskExecutor;
    private final RabbitTemplate rabbitTemplate;
    private final StudentAttendanceRepository studentAttendanceRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthTokenRepository authTokenRepository;

    public void enqueueCompletedSessionReport(UUID sessionId) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.CLASS_SESSION_COMPLETED_NOTIFICATION_ROUTING_KEY,
                    new ClassSessionCompletedNotificationMessage(sessionId)
            );
        } catch (AmqpException e) {
            log.error("Failed to publish completed session notification to RabbitMQ. Falling back to local queue. sessionId={}",
                    sessionId, e);
            notificationTaskExecutor.submit(sessionId, () -> processCompletedSessionNotifications(sessionId));
        }
    }

    @RabbitListener(
            queues = RabbitMQConfig.CLASS_SESSION_COMPLETED_NOTIFICATION_QUEUE,
            errorHandler = "rabbitMQErrorHandler"
    )
    public void onCompletedSessionNotificationMessage(ClassSessionCompletedNotificationMessage message) {
        if (message == null || message.sessionId() == null) {
            throw new IllegalArgumentException("Completed session notification message must include sessionId");
        }

        processCompletedSessionNotifications(message.sessionId());
    }

    public void processCompletedSessionNotifications(UUID sessionId) {
        sendCompletedSessionReport(sessionId);
        sendCompletedSessionAttendanceNotifications(sessionId);
    }

    public void sendCompletedSessionReport(UUID sessionId) {
        UUID notificationId = null;

        try {
            ClassSessionReportPayload report = classSessionReportService.buildReport(sessionId);
            Set<UUID> recipientUserIds = resolveRecipientUserIds(report);

            if (recipientUserIds.isEmpty()) {
                log.warn("Completed session report has no recipients. sessionId={}, classScheduleId={}",
                        sessionId, report.classScheduleId());
                return;
            }

            Optional<NotificationDTO.NotificationResponse> notificationOptional =
                    notificationService.createClassSessionReportIfAbsent(
                            report.title(),
                            report.body(),
                            sessionId.toString(),
                            report.data(),
                            recipientUserIds.stream().toList()
                    );

            if (notificationOptional.isEmpty()) {
                log.info("Completed session report already exists. sessionId={}", sessionId);
                return;
            }

            notificationId = notificationOptional.get().getNotificationId();
            List<String> tokens = resolveFcmTokens(recipientUserIds);

            if (tokens.isEmpty()) {
                log.warn("Completed session report saved but no active FCM tokens were found. notificationId={}, sessionId={}",
                        notificationId, sessionId);
                return;
            }

            FirebaseMulticastResult result = firebaseNotificationSender.send(new AttendanceNotificationPayload(
                    notificationId,
                    tokens,
                    report.title(),
                    report.body(),
                    report.data()
            ));

            if (!result.attempted()) {
                log.info("Firebase send was not attempted for completed session report. notificationId={}, sessionId={}",
                        notificationId, sessionId);
                return;
            }

            if (result.hasSuccess()) {
                notificationStatusService.markSent(notificationId);
                log.info("Completed session report sent. notificationId={}, sessionId={}, success={}/{}",
                        notificationId, sessionId, result.successCount(), result.attemptedCount());
                return;
            }

            notificationStatusService.markFailed(notificationId);
            log.warn("Completed session report failed for all tokens. notificationId={}, sessionId={}, failures={}",
                    notificationId, sessionId, result.failureCount());
        } catch (Exception e) {
            if (notificationId != null) {
                notificationStatusService.markFailed(notificationId);
            }
            log.error("Failed to process completed session report. sessionId={}", sessionId, e);
        }
    }

    public void sendCompletedSessionAttendanceNotifications(UUID sessionId) {
        List<CompletedSessionAttendanceNotificationRow> rows =
                studentAttendanceRepository.findCompletedSessionAttendanceNotificationRows(sessionId);

        if (rows.isEmpty()) {
            log.info("No attendance records found for completed session notifications. sessionId={}", sessionId);
            return;
        }

        int successCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (CompletedSessionAttendanceNotificationRow row : rows) {
            try {
                if (row.getStudentPersonId() == null) {
                    skippedCount++;
                    log.warn("Skipping completed session attendance notification because studentPersonId is missing. attendanceId={}",
                            row.getAttendanceId());
                    continue;
                }

                NotificationContent content = buildCompletedSessionAttendanceContent(row);
                List<UUID> recipientUserIds = userProfileRepository.findActiveUserIdsByPersonId(row.getStudentPersonId())
                        .stream()
                        .distinct()
                        .toList();

                if (recipientUserIds.isEmpty()) {
                    skippedCount++;
                    log.info("Skipping completed session attendance notification because no recipient user exists. attendanceId={}",
                            row.getAttendanceId());
                    continue;
                }

                Map<String, String> data = buildCompletedSessionAttendanceData(row);
                Optional<NotificationDTO.NotificationResponse> notificationOptional =
                        notificationService.createClassSessionAttendanceResultIfAbsent(
                                content.title(),
                                content.body(),
                                row.getAttendanceId().toString(),
                                data,
                                recipientUserIds
                        );

                if (notificationOptional.isEmpty()) {
                    skippedCount++;
                    log.info("Completed session attendance notification already exists. attendanceId={}", row.getAttendanceId());
                    continue;
                }

                UUID notificationId = notificationOptional.get().getNotificationId();
                List<String> tokens = authTokenRepository.findActiveFcmTokens(row.getStudentPersonId()).stream()
                        .filter(token -> token != null && !token.isBlank())
                        .map(String::trim)
                        .distinct()
                        .toList();

                if (tokens.isEmpty()) {
                    skippedCount++;
                    log.info("Completed session attendance notification saved but no active FCM tokens found. notificationId={}, attendanceId={}",
                            notificationId, row.getAttendanceId());
                    continue;
                }

                FirebaseMulticastResult result = firebaseNotificationSender.send(new AttendanceNotificationPayload(
                        notificationId,
                        tokens,
                        content.title(),
                        content.body(),
                        data
                ));

                if (!result.attempted()) {
                    skippedCount++;
                    log.info("Firebase send was not attempted for completed session attendance notification. notificationId={}, attendanceId={}",
                            notificationId, row.getAttendanceId());
                    continue;
                }

                if (result.hasSuccess()) {
                    successCount++;
                    notificationStatusService.markSent(notificationId);
                    log.info("Completed session attendance notification sent. notificationId={}, attendanceId={}, success={}/{}",
                            notificationId, row.getAttendanceId(), result.successCount(), result.attemptedCount());
                    continue;
                }

                failedCount++;
                notificationStatusService.markFailed(notificationId);
                log.warn("Completed session attendance notification failed for all tokens. notificationId={}, attendanceId={}, failures={}",
                        notificationId, row.getAttendanceId(), result.failureCount());
            } catch (Exception e) {
                failedCount++;
                log.error("Failed to process completed session attendance notification. attendanceId={}",
                        row.getAttendanceId(), e);
            }
        }

        log.info("Completed session attendance notifications finished. sessionId={}, sent={}, skipped={}, failed={}",
                sessionId, successCount, skippedCount, failedCount);
    }

    private Set<UUID> resolveRecipientUserIds(ClassSessionReportPayload report) {
        Set<UUID> recipientUserIds = new LinkedHashSet<>(userRepository.findUserIdsByRoleCode(HEAD_COACH_ROLE_CODE));

        if (!report.coachPersonIds().isEmpty()) {
            recipientUserIds.addAll(userProfileRepository.findActiveUserIdsByPersonIds(report.coachPersonIds()));
        }

        return recipientUserIds;
    }

    private List<String> resolveFcmTokens(Set<UUID> recipientUserIds) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            return List.of();
        }

        return authTokenRepository.findActiveFcmTokensByUserIds(recipientUserIds).stream()
                .filter(token -> token != null && !token.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private NotificationContent buildCompletedSessionAttendanceContent(CompletedSessionAttendanceNotificationRow row) {
        String studentName = row.getStudentName() == null || row.getStudentName().isBlank()
                ? "học viên"
                : row.getStudentName();
        String scheduleId = row.getScheduleId() == null || row.getScheduleId().isBlank()
                ? "chưa cập nhật"
                : row.getScheduleId();
        String sessionDate = row.getSessionDate() == null
                ? "chưa cập nhật"
                : row.getSessionDate().format(DATE_FORMATTER);
        String attendanceTime = formatAttendanceTime(row);
        String attendanceStatus = formatAttendanceStatus(row);
        String evaluationStatus = formatEvaluationStatus(row);

        StringBuilder body = new StringBuilder(String.format("""
                Học viên %s đã hoàn thành buổi học.
                Lớp: %s
                Ngày học: %s
                Giờ điểm danh: %s
                Trạng thái điểm danh: %s
                Đánh giá buổi học: %s""",
                studentName,
                scheduleId,
                sessionDate,
                attendanceTime,
                attendanceStatus,
                evaluationStatus
        ));

        if (row.getNote() != null && !row.getNote().isBlank()) {
            body.append(System.lineSeparator())
                    .append("Ghi chú: ")
                    .append(row.getNote().trim());
        }

        return new NotificationContent("Kết quả buổi học", body.toString());
    }

    private Map<String, String> buildCompletedSessionAttendanceData(CompletedSessionAttendanceNotificationRow row) {
        Map<String, String> data = new HashMap<>();
        data.put("screen", "AttendanceHistory");
        data.put("attendanceId", row.getAttendanceId().toString());
        data.put("sessionId", row.getSessionId().toString());
        data.put("personId", row.getStudentPersonId().toString());
        if (row.getScheduleId() != null) {
            data.put("classScheduleId", row.getScheduleId());
        }
        if (row.getSessionDate() != null) {
            data.put("sessionDate", row.getSessionDate().toString());
        }
        if (row.getAttendanceStatus() != null) {
            data.put("attendanceStatus", row.getAttendanceStatus().name());
        }
        if (row.getEvaluationStatus() != null) {
            data.put("evaluationStatus", row.getEvaluationStatus().name());
        }
        if (row.getNote() != null && !row.getNote().isBlank()) {
            data.put("note", row.getNote().trim());
        }
        return Map.copyOf(data);
    }

    private String formatAttendanceTime(CompletedSessionAttendanceNotificationRow row) {
        LocalDateTime time = row.getCheckInTime() != null ? row.getCheckInTime() : row.getCreatedAt();
        if (time == null || row.getCheckInTime() == null) {
            return "Chưa ghi nhận";
        }

        return time.atZone(HO_CHI_MINH_ZONE).format(DATE_TIME_FORMATTER);
    }

    private String formatAttendanceStatus(CompletedSessionAttendanceNotificationRow row) {
        if (row.getAttendanceStatus() == null) {
            return "Chưa cập nhật";
        }

        return switch (row.getAttendanceStatus()) {
            case PRESENT -> "Có mặt";
            case LATE -> "Đi muộn";
            case ABSENT -> "Vắng mặt";
            case EXCUSED -> "Vắng có phép";
            case MAKEUP -> "Học bù";
        };
    }

    private String formatEvaluationStatus(CompletedSessionAttendanceNotificationRow row) {
        if (row.getEvaluationStatus() == null) {
            return "Chưa đánh giá";
        }

        return switch (row.getEvaluationStatus()) {
            case PENDING -> "Chưa đánh giá";
            case GOOD -> "Tốt";
            case AVERAGE -> "Trung bình";
            case WEAK -> "Yếu";
        };
    }

    private record NotificationContent(String title, String body) {
    }
}
