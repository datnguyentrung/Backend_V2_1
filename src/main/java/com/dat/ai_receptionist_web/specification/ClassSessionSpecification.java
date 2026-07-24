package com.dat.ai_receptionist_web.specification;

import com.dat.ai_receptionist_web.domain.Operation.ClassSession;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

public class ClassSessionSpecification {
    public static Specification<ClassSession> hasSearch(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) return null;
            // Ví dụ: Tìm theo ghi chú (note) hoặc ID lịch học
            String pattern = "%" + search.toLowerCase().trim() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("note")), pattern),
                    cb.like(cb.lower(root.join("classSchedule").get("scheduleId")), pattern)
            );
        };
    }

    public static Specification<ClassSession> hasDate(LocalDate sessionDate) {
        return (root, query, cb) ->
                sessionDate == null ? null : cb.equal(root.get("sessionDate"), sessionDate);
    }

    public static Specification<ClassSession> hasAttendanceStatus(Boolean isClosed) {
        return (root, query, cb) ->
                isClosed == null ? null : cb.equal(root.get("isAttendanceClosed"), isClosed);
    }

    public static Specification<ClassSession> hasScheduleIds(List<String> scheduleIds) {
        return (root, query, cb) ->
                (scheduleIds == null || scheduleIds.isEmpty()) ? null : root.join("classSchedule").get("scheduleId").in(scheduleIds);
    }
}
