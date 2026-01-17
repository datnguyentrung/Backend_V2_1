package com.dat.backend_v2_1.util.Helper;

import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class DateSpecification {
    public static <T> Specification<T> hasMonthOfDateIn(List<Integer> months, String dateFieldName) {
        return (root, query, cb) -> {
            if (months == null || months.isEmpty()) return null;

            List<Double> monthDoubles = months.stream()
                    .map(Integer::doubleValue)
                    .toList();

            return cb.function(
                    "date_part", Double.class,
                    cb.literal("month"),
                    root.get(dateFieldName)
            ).in(monthDoubles);
        };
    }

    public static <T> Specification<T> hasYearOfDate(int year, String dateFieldName) {
        return (root, query, cb) -> cb.equal(
                cb.function(
                        "date_part", Double.class,
                        cb.literal("year"),
                        root.get(dateFieldName)
                ),
                (double) year
        );
    }

    public static <T> Specification<T> hasMonthOfDateInWithRelation(List<Integer> months, String relation, String dateField) {
        return (root, query, cb) -> {
            if (months == null || months.isEmpty()) return null;

            List<Double> monthDoubles = months.stream()
                    .map(Integer::doubleValue)
                    .toList();

            Join<Object, Object> join = root.join(relation);
            return cb.function(
                    "date_part", Double.class,
                    cb.literal("month"),
                    join.get(dateField)
            ).in(monthDoubles);
        };
    }

    public static <T> Specification<T> hasYearOfDateWithRelation(int year, String relation, String dateField) {
        return (root, query, cb) -> {
            Join<Object, Object> join = root.join(relation);
            return cb.equal(
                    cb.function(
                            "date_part", Double.class,
                            cb.literal("year"),
                            join.get(dateField)
                    ),
                    (double) year
            );
        };
    }

}
