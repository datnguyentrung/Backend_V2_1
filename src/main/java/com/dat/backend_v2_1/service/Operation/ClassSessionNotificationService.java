package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.dto.Operation.AttendanceNotificationPayload;
import com.dat.backend_v2_1.dto.Operation.ClassSessionReportPayload;
import com.dat.backend_v2_1.dto.Operation.FirebaseMulticastResult;
import com.dat.backend_v2_1.dto.Operation.NotificationDTO;
import com.dat.backend_v2_1.repository.Security.AuthTokenRepository;
import com.dat.backend_v2_1.repository.Security.UserProfileRepository;
import com.dat.backend_v2_1.repository.Security.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassSessionNotificationService {

    private static final String HEAD_COACH_ROLE_CODE = "HEAD_COACH";

    private final ClassSessionReportService classSessionReportService;
    private final NotificationService notificationService;
    private final FirebaseNotificationSender firebaseNotificationSender;
    private final NotificationStatusService notificationStatusService;
    private final AttendanceNotificationTaskExecutor notificationTaskExecutor;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthTokenRepository authTokenRepository;

    public void enqueueCompletedSessionReport(UUID sessionId) {
        notificationTaskExecutor.submit(sessionId, () -> sendCompletedSessionReport(sessionId));
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
}
