package com.dat.ai_receptionist_web.repository.Operation;

import com.dat.ai_receptionist_web.domain.Core.Student;
import com.dat.ai_receptionist_web.domain.Operation.StudentAttendance;
import com.dat.ai_receptionist_web.domain.Operation.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Operation.StudentAttendanceDTO;
import com.dat.ai_receptionist_web.enums.Operation.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Operation.EvaluationStatus;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // --- Định nghĩa các Expression cơ bản ---
        Expression<Long> totalExp = cb.count(root);

        // Attendance Cases
        Expression<Integer> presentCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.PRESENT);
        Expression<Integer> absentCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.ABSENT);
        Expression<Integer> excusedCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.EXCUSED);
        Expression<Integer> makeupCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.MAKEUP);
        Expression<Integer> lateCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.LATE);

        // Evaluation Cases
        Expression<Integer> goodCase = createCase(cb, root.get("evaluationStatus"), EvaluationStatus.GOOD);
        Expression<Integer> avgCase = createCase(cb, root.get("evaluationStatus"), EvaluationStatus.AVERAGE);
        Expression<Integer> weakCase = createCase(cb, root.get("evaluationStatus"), EvaluationStatus.WEAK);

        // Logic "Chưa đánh giá" (Pending)
        // Điều kiện 1: Đã đi học (Present, Makeup, Late)
        CriteriaBuilder.In<AttendanceStatus> attendedIn = cb.in(root.get("attendanceStatus"));
        attendedIn.value(AttendanceStatus.PRESENT)
                .value(AttendanceStatus.MAKEUP)
                .value(AttendanceStatus.LATE);

        // Điều kiện 2: Trạng thái đánh giá là PENDING hoặc NULL
        Predicate evaluationIsPendingOrNull = cb.or(
                cb.equal(root.get("evaluationStatus"), EvaluationStatus.PENDING),
                cb.isNull(root.get("evaluationStatus"))
        );

        // Gộp điều kiện Pending
        Expression<Integer> pendingCase = cb.<Integer>selectCase()
                .when(cb.and(attendedIn, evaluationIsPendingOrNull), 1)
                .otherwise(0);

        // --- Select tất cả trong 1 query (Tối ưu I/O DB) ---
        query.select(cb.tuple(
                totalExp, cb.sumAsLong(presentCase), cb.sumAsLong(absentCase),
                cb.sumAsLong(excusedCase), cb.sumAsLong(makeupCase), cb.sumAsLong(lateCase),
                cb.sumAsLong(goodCase), cb.sumAsLong(avgCase), cb.sumAsLong(weakCase),
                cb.sumAsLong(pendingCase)
        ));

        Tuple result = entityManager.createQuery(query).getSingleResult();

        // --- Bóc tách dữ liệu ---
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

    // File: StudentAttendanceRepositoryCustomImpl.java

    @Override
    public Map<String, StudentAttendanceDTO.AttendanceStats> getStatisticsGroupedByStudent(
            LocalDate startDate,
            LocalDate endDate,
            Collection<String> studentCodes
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<StudentAttendance> root = query.from(StudentAttendance.class);

        // JOIN để lấy được mã học viên
        Join<StudentAttendance, StudentEnrollment> enrollmentJoin = root.join("studentEnrollment", JoinType.INNER);
        Join<StudentEnrollment, Student> studentJoin = enrollmentJoin.join("student", JoinType.INNER);

        // Điều kiện: Lọc theo khoảng thời gian (Quý)
        List<Predicate> predicates = new ArrayList<>();
        if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("sessionDate"), startDate));
        if (endDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("sessionDate"), endDate));
        if (studentCodes != null && !studentCodes.isEmpty()) {
            predicates.add(studentJoin.get("studentCode").in(studentCodes));
        }

        query.where(cb.and(predicates.toArray(new Predicate[0])));

        // --- Định nghĩa các Expression ---
        Expression<Long> totalExp = cb.count(root);

        // Tái sử dụng lại các hàm createCase của bạn
        Expression<Integer> presentCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.PRESENT);
        Expression<Integer> absentCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.ABSENT);
        Expression<Integer> excusedCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.EXCUSED);
        Expression<Integer> makeupCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.MAKEUP);
        Expression<Integer> lateCase = createCase(cb, root.get("attendanceStatus"), AttendanceStatus.LATE);

        Expression<Integer> goodCase = createCase(cb, root.get("evaluationStatus"), EvaluationStatus.GOOD);
        Expression<Integer> avgCase = createCase(cb, root.get("evaluationStatus"), EvaluationStatus.AVERAGE);
        Expression<Integer> weakCase = createCase(cb, root.get("evaluationStatus"), EvaluationStatus.WEAK);

        // Logic "Chưa đánh giá" (Pending)
        CriteriaBuilder.In<AttendanceStatus> attendedIn = cb.in(root.get("attendanceStatus"));
        attendedIn.value(AttendanceStatus.PRESENT)
                .value(AttendanceStatus.MAKEUP)
                .value(AttendanceStatus.LATE);

        Predicate evaluationIsPendingOrNull = cb.or(
                cb.equal(root.get("evaluationStatus"), EvaluationStatus.PENDING),
                cb.isNull(root.get("evaluationStatus"))
        );

        Expression<Integer> pendingCase = cb.<Integer>selectCase()
                .when(cb.and(attendedIn, evaluationIsPendingOrNull), 1)
                .otherwise(0);

        // --- Select GỒM CẢ studentCode VÀ GROUP BY ---
        Path<String> studentCodePath = studentJoin.get("studentCode");

        query.select(cb.tuple(
                studentCodePath, // Index 0
                totalExp, cb.sumAsLong(presentCase), cb.sumAsLong(absentCase),
                cb.sumAsLong(excusedCase), cb.sumAsLong(makeupCase), cb.sumAsLong(lateCase),
                cb.sumAsLong(goodCase), cb.sumAsLong(avgCase), cb.sumAsLong(weakCase),
                cb.sumAsLong(pendingCase) // Index 10
        ));

        // GROUP BY TẠI ĐÂY
        query.groupBy(studentCodePath);

        List<Tuple> results = entityManager.createQuery(query).getResultList();

        // --- Chuyển kết quả thành Map ---
        Map<String, StudentAttendanceDTO.AttendanceStats> statsMap = new HashMap<>();

        for (Tuple result : results) {
            String studentCode = result.get(0, String.class);
            long total = getLong(result, 1);
            long present = getLong(result, 2);
            long absent = getLong(result, 3);
            long excused = getLong(result, 4);
            long makeup = getLong(result, 5);
            long late = getLong(result, 6);
            long good = getLong(result, 7);
            long avg = getLong(result, 8);
            long weak = getLong(result, 9);
            long pending = getLong(result, 10);

            long totalAttended = present + late + makeup;
            double rate = total == 0 ? 0.0 : Math.round(((double) totalAttended / total) * 1000.0) / 10.0;

            StudentAttendanceDTO.AttendanceStats stats = StudentAttendanceDTO.AttendanceStats.builder()
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

            statsMap.put(studentCode, stats);
        }

        return statsMap;
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
