package com.dat.ai_receptionist_web.specification;

import com.dat.ai_receptionist_web.domain.Core.Branch;
import com.dat.ai_receptionist_web.domain.Core.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Core.Coach;
import com.dat.ai_receptionist_web.domain.Operation.CoachAssignment;
import com.dat.ai_receptionist_web.enums.Operation.CoachAssignmentStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CoachAssignmentSpecification {

    public static Specification<CoachAssignment> filterBy(
            UUID coachId,
            String classScheduleId,
            Integer branchId,
            CoachAssignmentStatus status,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate effectiveDate,
            String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<CoachAssignment, Coach> coachJoin = root.join("coach", JoinType.INNER);
            Join<CoachAssignment, ClassSchedule> scheduleJoin = root.join("classSchedule", JoinType.INNER);
            Join<ClassSchedule, Branch> branchJoin = scheduleJoin.join("branch", JoinType.LEFT);

            if (coachId != null) {
                predicates.add(cb.equal(coachJoin.get("personId"), coachId));
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
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("assignedDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("assignedDate"), endDate));
            }
            if (effectiveDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("assignedDate"), effectiveDate));
                predicates.add(cb.or(
                        cb.isNull(root.get("endDate")),
                        cb.greaterThanOrEqualTo(root.get("endDate"), effectiveDate)
                ));
                predicates.add(cb.equal(root.get("status"), CoachAssignmentStatus.ACTIVE));
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
