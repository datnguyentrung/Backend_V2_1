package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.StudentAttendance;
import com.dat.backend_v2_1.dto.Operation.StudentAttendanceDTO;
import com.dat.backend_v2_1.enums.Operation.AttendanceStatus;
import com.dat.backend_v2_1.enums.Operation.EvaluationStatus;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class StudentAttendanceRepositoryCustomImpl implements StudentAttendanceRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<StudentAttendance> findAllWithEntityGraph(Specification<StudentAttendance> spec, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. COUNT QUERY
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<StudentAttendance> countRoot = countQuery.from(StudentAttendance.class);
        if (spec != null) {
            Predicate predicate = spec.toPredicate(countRoot, countQuery, cb);
            if (predicate != null) countQuery.where(predicate);
        }
        countQuery.select(cb.count(countRoot));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        if (total == 0) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        // 2. DATA QUERY
        CriteriaQuery<StudentAttendance> dataQuery = cb.createQuery(StudentAttendance.class);
        Root<StudentAttendance> dataRoot = dataQuery.from(StudentAttendance.class);
        if (spec != null) {
            Predicate predicate = spec.toPredicate(dataRoot, dataQuery, cb);
            if (predicate != null) dataQuery.where(predicate);
        }
        dataQuery.select(dataRoot);

        // Xử lý Sorting (Hỗ trợ cả nested property nếu cần)
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            for (Sort.Order sortOrder : pageable.getSort()) {
                Path<Object> path = getPath(dataRoot, sortOrder.getProperty());
                orders.add(sortOrder.isAscending() ? cb.asc(path) : cb.desc(path));
            }
            dataQuery.orderBy(orders);
        }

        TypedQuery<StudentAttendance> typedQuery = entityManager.createQuery(dataQuery);

        // Apply EntityGraph
        EntityGraph<?> entityGraph = entityManager.getEntityGraph("StudentAttendance.withDetails");
        typedQuery.setHint("jakarta.persistence.fetchgraph", entityGraph);

        // Apply Pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<StudentAttendance> content = typedQuery.getResultList();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public StudentAttendanceDTO.AttendanceStats getStatistics(Specification<StudentAttendance> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<StudentAttendance> root = query.from(StudentAttendance.class);

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) query.where(predicate);
        }

        // --- Định nghĩa các Expression ---
        Expression<Long> totalExp = cb.count(root);

        // Helper function để tạo CASE WHEN SUM
        // Attendance
        Expression<Integer> presentCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.PRESENT);
        Expression<Integer> absentCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.ABSENT);
        Expression<Integer> excusedCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.EXCUSED);
        Expression<Integer> makeupCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.MAKEUP);
        Expression<Integer> lateCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.LATE);

        // Evaluation
        Expression<Integer> goodCase = createCase(cb, root.get("evaluationStatus"), EvaluationStatus.GOOD);
        Expression<Integer> avgCase = createCase(cb, root.get("evaluationStatus"), EvaluationStatus.AVERAGE);
        Expression<Integer> weakCase = createCase(cb, root.get("evaluationStatus"), EvaluationStatus.WEAK);

        // Logic "Chưa đánh giá" (Chỉ tính người đi học: Present, Makeup, Late)
        List<AttendanceStatus> attendedStatuses = List.of(AttendanceStatus.PRESENT, AttendanceStatus.MAKEUP, AttendanceStatus.LATE);
        Expression<Integer> pendingCase = cb.<Integer>selectCase()
                .when(cb.and(
                        root.get("attendanceStatus").in(attendedStatuses),
                        cb.or(cb.equal(root.get("evaluationStatus"), EvaluationStatus.PENDING), cb.isNull(root.get("evaluationStatus")))
                ), 1)
                .otherwise(0);

        // Select tất cả trong 1 Tuple
        query.select(cb.tuple(
                totalExp,                       // 0
                cb.sumAsLong(presentCase),      // 1
                cb.sumAsLong(absentCase),       // 2
                cb.sumAsLong(excusedCase),      // 3
                cb.sumAsLong(makeupCase),       // 4
                cb.sumAsLong(lateCase),         // 5
                cb.sumAsLong(goodCase),         // 6
                cb.sumAsLong(avgCase),          // 7
                cb.sumAsLong(weakCase),         // 8
                cb.sumAsLong(pendingCase)       // 9
        ));

        Tuple result = entityManager.createQuery(query).getSingleResult();

        // --- Bóc tách và gán giá trị (Null-safe) ---
        long total = getLong(result, 0);
        long present = getLong(result, 1);
        long absent = getLong(result, 2);
        long excused = getLong(result, 3);
        long makeup = getLong(result, 4);
        long late = getLong(result, 5);
        long good = getLong(result, 6);
        long avg = getLong(result, 7);
        long weak = getLong(result, 8);
        long pending = getLong(result, 9);

        // Tính Rate
        long totalAttended = present + late + makeup;
        double rate = total == 0 ? 0.0 : Math.round(((double) totalAttended / total) * 1000.0) / 10.0;

        return StudentAttendanceDTO.AttendanceStats.builder()
                .totalRecords(total)
                .attendanceRate(rate)
                .presentCount(present)
                .absentCount(absent)
                .excusedCount(excused)
                .makeupCount(makeup)
                .lateCount(late)
                .evalGoodCount(good)
                .evalAverageCount(avg)
                .evalWeakCount(weak)
                .evalPendingCount(pending)
                .build();
    }

    // --- HELPER METHODS ---

    private Expression<Integer> createCase(CriteriaBuilder cb, Expression<?> path, Object value) {
        return cb.<Integer>selectCase().when(cb.equal(path, value), 1).otherwise(0);
    }

    private Long getLong(Tuple result, int index) {
        Object val = result.get(index);
        if (val == null) return 0L;
        if (val instanceof Long) return (Long) val;
        return ((Number) val).longValue();
    }

    /**
     * Hỗ trợ lấy Path cho các thuộc tính lồng nhau (vd: "studentEnrollment.student.fullName")
     */
    private Path<Object> getPath(Root<StudentAttendance> root, String property) {
        String[] parts = property.split("\\.");
        Path<Object> path = root.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            path = path.get(parts[i]);
        }
        return path;
    }
}