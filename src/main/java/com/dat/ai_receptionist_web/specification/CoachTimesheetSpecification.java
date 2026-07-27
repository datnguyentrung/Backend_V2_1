package com.dat.ai_receptionist_web.specification;

import com.dat.ai_receptionist_web.domain.Core.Branch;
import com.dat.ai_receptionist_web.domain.Core.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Core.Coach;
import com.dat.ai_receptionist_web.domain.Operation.ClassSession;
import com.dat.ai_receptionist_web.domain.Operation.CoachTimesheet;
import com.dat.ai_receptionist_web.enums.Operation.CoachTimesheetStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CoachTimesheetSpecification {

    public static Specification<CoachTimesheet> filterBy(
            UUID coachId,
            UUID classSessionId,
            String classScheduleId,
            Integer branchId,
            CoachTimesheetStatus status,
            LocalDate workDate,
            LocalDate fromDate,
            LocalDate toDate,
            YearMonth month,
            String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<CoachTimesheet, Coach> coachJoin = root.join("coach", JoinType.INNER);
            Join<CoachTimesheet, ClassSession> sessionJoin = root.join("classSession", JoinType.INNER);
            Join<ClassSession, ClassSchedule> scheduleJoin = sessionJoin.join("classSchedule", JoinType.INNER);
            Join<ClassSchedule, Branch> branchJoin = scheduleJoin.join("branch", JoinType.LEFT);

            if (coachId != null) {
                predicates.add(cb.equal(coachJoin.get("personId"), coachId));
            }
            if (classSessionId != null) {
                predicates.add(cb.equal(sessionJoin.get("sessionId"), classSessionId));
            }
            if (classScheduleId != null && !classScheduleId.isBlank()) {
                predicates.add(cb.equal(scheduleJoin.get("scheduleId"), classScheduleId.trim()));
            }
            if (branchId != null) {
                predicates.add(cb.equal(branchJoin.get("branchId"), branchId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (workDate != null) {
                predicates.add(cb.equal(root.get("workingDate"), workDate));
            } else if (month != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("workingDate"), month.atDay(1)));
                predicates.add(cb.lessThanOrEqualTo(root.get("workingDate"), month.atEndOfMonth()));
            } else {
                if (fromDate != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("workingDate"), fromDate));
                }
                if (toDate != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("workingDate"), toDate));
                }
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(coachJoin.get("fullName")), pattern),
                        cb.like(cb.lower(coachJoin.get("staffCode")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
