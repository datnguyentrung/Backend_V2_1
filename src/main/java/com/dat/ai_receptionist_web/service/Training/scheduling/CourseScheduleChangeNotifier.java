package com.dat.ai_receptionist_web.service.Training.scheduling;

import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import com.dat.ai_receptionist_web.enums.Training.LeaveRequestStatus;
import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import com.dat.ai_receptionist_web.enums.Training.ScheduleImpactType;
import com.dat.ai_receptionist_web.service.Notification.NotificationService;
import com.dat.ai_receptionist_web.service.Notification.TransactionAfterCommitExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Nhận danh sách LeaveRequest bị ảnh hưởng (phát hiện trong transaction) và
 * gửi một notification CLASS_SCHEDULE cho SYSTEM_ADMIN sau commit. Không persist
 * lịch sử impact/audit. Payload chỉ chứa ID nghiệp vụ và trạng thái đơn (không PII).
 */
@Component
@RequiredArgsConstructor
public class CourseScheduleChangeNotifier {
    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";

    private final TransactionAfterCommitExecutor afterCommitExecutor;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public void notifyAfterCommit(UUID courseId, List<AffectedLeaveRequest> affected) {
        if (affected == null || affected.isEmpty()) {
            return;
        }
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(
                    affected.stream().map(this::toPayload).toList());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize schedule impact payload", e);
        }

        afterCommitExecutor.afterCommit(() -> notificationService.create(
                new NotificationDTO.CreateRequest(
                        "Lịch học thay đổi",
                        "Đổi lịch khóa học làm hủy buổi học liên quan đến đơn xin nghỉ.",
                        NotificationType.CLASS_SCHEDULE,
                        "COURSE_SCHEDULE_CHANGE",
                        courseId.toString(),
                        payloadJson,
                        null,
                        null,
                        Set.of(SYSTEM_ADMIN_ROLE)
                )
        ));
    }

    private Map<String, Object> toPayload(AffectedLeaveRequest affected) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("leaveRequestId", affected.leaveRequestId());
        values.put("personId", affected.personId());
        values.put("courseId", affected.courseId());
        values.put("classSessionId", affected.classSessionId());
        values.put("impactType", affected.impactType().name());
        values.put("requestStatus", affected.requestStatus().name());
        return values;
    }

    public record AffectedLeaveRequest(
            UUID leaveRequestId,
            UUID personId,
            UUID courseId,
            UUID classSessionId,
            ScheduleImpactType impactType,
            LeaveRequestStatus requestStatus) {
    }
}
