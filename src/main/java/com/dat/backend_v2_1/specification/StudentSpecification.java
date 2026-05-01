package com.dat.backend_v2_1.specification;

import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.domain.Operation.StudentEnrollment;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StudentSpecification {

    private StudentSpecification() {
    }

    public static Specification<Student> filterBy(
            String search,
            StudentStatus status,
            List<String> scheduleIds
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Search by fullName, studentCode, phoneNumber (case-insensitive LIKE)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";

                Predicate nameMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("fullName")),
                        searchPattern
                );
                Predicate codeMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("studentCode")),
                        searchPattern
                );
                Predicate phoneMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("phoneNumber")),
                        searchPattern
                );

                predicates.add(criteriaBuilder.or(nameMatch, codeMatch, phoneMatch));
            }

            // 2. Filter by studentStatus
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("studentStatus"), status));
            }

            // 3. Filter by scheduleIds via StudentEnrollment (subquery since Student has no enrollments collection)
            if (scheduleIds != null && !scheduleIds.isEmpty()) {
                predicates.add(buildScheduleFilter(root, query, criteriaBuilder, scheduleIds));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification without status filter — used for counting stats across all statuses.
     */
    public static Specification<Student> filterWithoutStatus(
            String search,
            List<String> scheduleIds
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";

                Predicate nameMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("fullName")),
                        searchPattern
                );
                Predicate codeMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("studentCode")),
                        searchPattern
                );
                Predicate phoneMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("phoneNumber")),
                        searchPattern
                );

                predicates.add(criteriaBuilder.or(nameMatch, codeMatch, phoneMatch));
            }

            if (scheduleIds != null && !scheduleIds.isEmpty()) {
                predicates.add(buildScheduleFilter(root, query, criteriaBuilder, scheduleIds));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate buildScheduleFilter(
            Root<Student> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            List<String> scheduleIds
    ) {
        Subquery<UUID> subquery = query.subquery(UUID.class);
        Root<StudentEnrollment> enrollmentRoot = subquery.from(StudentEnrollment.class);

        subquery.select(enrollmentRoot.get("student").get("userId"))
                .where(
                        criteriaBuilder.equal(enrollmentRoot.get("student"), root),
                        enrollmentRoot.get("classSchedule").get("scheduleId").in(scheduleIds)
                );

        return criteriaBuilder.exists(subquery);
    }
}
