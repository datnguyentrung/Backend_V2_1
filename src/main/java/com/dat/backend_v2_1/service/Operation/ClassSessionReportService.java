package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.dto.Operation.ClassSessionReportPayload;
import com.dat.backend_v2_1.dto.Operation.ClassSessionReportSessionRow;
import com.dat.backend_v2_1.dto.Operation.CoachTimesheetStatusProjection;
import com.dat.backend_v2_1.dto.Operation.ResponsibleCoachProjection;
import com.dat.backend_v2_1.dto.Operation.StudentAttendanceDTO;
import com.dat.backend_v2_1.repository.Operation.ClassSessionRepository;
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

    private final ClassSessionRepository classSessionRepository;
    private final StudentAttendanceService studentAttendanceService;
    private final CoachAssignmentService coachAssignmentService;
    private final CoachTimesheetService coachTimesheetService;

    @Transactional(readOnly = true)
    public ClassSessionReportPayload buildReport(UUID sessionId) {
        ClassSessionReportSessionRow session = classSessionRepository.findReportSessionRow(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Class session not found: " + sessionId));

        StudentAttendanceDTO.AttendanceStats stats = studentAttendanceService.getStatsBySessionId(sessionId);
        List<ResponsibleCoachProjection> coaches = coachAssignmentService.findResponsibleCoaches(
                session.getClassScheduleId(),
                session.getSessionDate()
        );

        List<UUID> assignmentIds = coaches.stream()
                .map(ResponsibleCoachProjection::getAssignmentId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, CoachTimesheetStatusProjection> timesheets =
                coachTimesheetService.findStatusesByAssignmentIds(assignmentIds, session.getSessionDate());

        String coachSummary = coachTimesheetService.buildResponsibleCoachReportSummary(coaches, timesheets);
        String formattedDate = session.getSessionDate()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")));
        long actualAttendanceCount = stats.getPresentCount() + stats.getLateCount() + stats.getMakeupCount();

        String title = String.format("Bao cao lop %s ngay %s", session.getClassScheduleId(), formattedDate);
        String body = String.format(
                Locale.forLanguageTag("vi-VN"),
                """
                Lop %s ngay %s da hoan thanh.

                Si so: %d hoc vien.
                Di hoc: %d - dung gio %d, di muon %d, hoc bu %d.
                Vang khong phep: %d; vang co phep: %d.
                Ty le di hoc: %.1f%%.

                Danh gia: tot %d, trung binh %d, yeu %d, chua danh gia %d.

                HLV phu trach: %s
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

        return new ClassSessionReportPayload(
                session.getSessionId(),
                session.getClassScheduleId(),
                title,
                body,
                Map.copyOf(data),
                coachPersonIds
        );
    }
}
