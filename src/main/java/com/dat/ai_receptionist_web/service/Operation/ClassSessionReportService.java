package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.dto.Operation.ClassSessionDTO;
import com.dat.ai_receptionist_web.dto.Operation.ResponsibleCoachProjection;
import com.dat.ai_receptionist_web.dto.Operation.StudentAttendanceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassSessionReportService {

    private final ClassSessionService classSessionService;
    private final CoachTimesheetService coachTimesheetService;

    @Transactional(readOnly = true)
    public ClassSessionDTO.ReportPayload buildReport(UUID sessionId) {
        ClassSessionDTO.ReportData reportData = classSessionService.getReportData(sessionId);
        ClassSessionDTO.ReportSessionRow session = reportData.session();
        StudentAttendanceDTO.AttendanceStats stats = reportData.attendanceStats();
        List<ResponsibleCoachProjection> coaches = reportData.responsibleCoaches();

        String coachSummary = coachTimesheetService.buildResponsibleCoachReportSummary(
                coaches,
                reportData.timesheetsByAssignmentId()
        );
        String formattedDate = session.getSessionDate()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")));
        long actualAttendanceCount = stats.getPresentCount() + stats.getLateCount() + stats.getMakeupCount();

        String title = String.format("Báo cáo lớp %s ngày %s", session.getClassScheduleId(), formattedDate);
        String body = String.format(
                Locale.forLanguageTag("vi-VN"),
                """
                Lớp %s ngày %s đã hoàn thành.

                Sĩ số: %d học viên.
                Đi học: %d - đúng giờ %d, đi muộn %d, học bù %d.
                Vắng không phép: %d; vắng có phép: %d.
                Tỷ lệ đi học: %.1f%%.

                Đánh giá: tốt %d, trung bình %d, yếu %d, chưa đánh giá %d.

                HLV phụ trách: %s
                """,
                session.getClassScheduleId(),
                formattedDate,
                stats.getTotalRecords(),
                actualAttendanceCount,
                stats.getPresentCount(),
                stats.getLateCount(),
                stats.getMakeupCount(),
                stats.getAbsentCount(),
                stats.getExcusedCount(),
                stats.getAttendanceRate(),
                stats.getEvalGoodCount(),
                stats.getEvalAverageCount(),
                stats.getEvalWeakCount(),
                stats.getEvalPendingCount(),
                coachSummary
        ).trim();

        Map<String, String> data = new HashMap<>();
        data.put("screen", "ClassSessionDetail");
        data.put("sessionId", session.getSessionId().toString());
        data.put("classScheduleId", session.getClassScheduleId());
        data.put("sessionDate", session.getSessionDate().toString());

        Set<UUID> coachPersonIds = coaches.stream()
                .map(ResponsibleCoachProjection::getCoachPersonId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        return new ClassSessionDTO.ReportPayload(
                session.getSessionId(),
                session.getClassScheduleId(),
                title,
                body,
                Map.copyOf(data),
                coachPersonIds
        );
    }
}
