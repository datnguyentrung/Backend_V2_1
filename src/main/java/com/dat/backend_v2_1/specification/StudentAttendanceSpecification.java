package com.dat.backend_v2_1.specification;

import com.dat.backend_v2_1.domain.Core.Branch;
import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.domain.Operation.StudentAttendance;
import com.dat.backend_v2_1.domain.Operation.StudentEnrollment;
import com.dat.backend_v2_1.enums.Core.Belt;
import com.dat.backend_v2_1.enums.Core.ScheduleLevel;
import com.dat.backend_v2_1.enums.Operation.AttendanceStatus;
import com.dat.backend_v2_1.enums.Operation.EvaluationStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Specification cho StudentAttendance - Giải quyết vấn đề Dynamic Filtering
 * <p>
 * Ưu điểm so với JPQL cứng:
 * 1. Type-safe: Compiler check tại compile-time
 * 2. Linh hoạt: Chỉ thêm điều kiện khi tham số không null/rỗng
 * 3. Tránh lỗi PostgreSQL "could not determine data type"
 * 4. Clean Code: Dễ đọc, dễ maintain, dễ test
 * 5. Reusable: Có thể kết hợp các Specification với nhau
 *
 * @author DAT Team
 * @since 2026-03-08
 */
@Slf4j
public class StudentAttendanceSpecification {

    /**
     * Main method: Kết hợp tất cả các điều kiện filter
     *
     * @param search             Tìm kiếm theo tên/mã/SĐT học viên
     * @param sessionDate        Ngày học
     * @param attendanceStatuses Danh sách trạng thái điểm danh
     * @param evaluationStatuses Danh sách trạng thái đánh giá
     * @param belts              Danh sách đai (belt)
     * @param branchIds          Danh sách chi nhánh
     * @param scheduleLevels     Danh sách cấp độ lớp
     * @param scheduleId         Mã lớp học
     * @return Specification kết hợp
     */
    public static Specification<StudentAttendance> filterBy(
            String search,
            LocalDate sessionDate,
            List<AttendanceStatus> attendanceStatuses,
            List<EvaluationStatus> evaluationStatuses,
            List<Belt> belts,
            List<Integer> branchIds,
            List<ScheduleLevel> scheduleLevels,
            String scheduleId
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Join các bảng liên quan (KHÔNG dùng JOIN FETCH vì sẽ xử lý sau bằng EntityGraph)
            Join<StudentAttendance, StudentEnrollment> enrollmentJoin = root.join("studentEnrollment", JoinType.INNER);
            Join<StudentEnrollment, Student> studentJoin = enrollmentJoin.join("student", JoinType.INNER);
            Join<StudentEnrollment, ClassSchedule> scheduleJoin = enrollmentJoin.join("classSchedule", JoinType.INNER);
            Join<ClassSchedule, Branch> branchJoin = scheduleJoin.join("branch", JoinType.INNER);

            // 1. Tìm kiếm chuỗi (Case-insensitive LIKE)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";

                Predicate nameMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(studentJoin.get("fullName")),
                        searchPattern
                );
                Predicate codeMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(studentJoin.get("studentCode")),
                        searchPattern
                );
                Predicate phoneMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(studentJoin.get("phoneNumber")),
                        searchPattern
                );

                predicates.add(criteriaBuilder.or(nameMatch, codeMatch, phoneMatch));
            }

            // 2. Filter theo ngày học
            if (sessionDate != null) {
                predicates.add(criteriaBuilder.equal(root.get("sessionDate"), sessionDate));
            }

            // 3. Filter theo trạng thái điểm danh (IN)
            if (attendanceStatuses != null && !attendanceStatuses.isEmpty()) {
                predicates.add(root.get("attendanceStatus").in(attendanceStatuses));
            }

            // 4. Filter theo trạng thái đánh giá (IN)
            if (evaluationStatuses != null && !evaluationStatuses.isEmpty()) {
                predicates.add(root.get("evaluationStatus").in(evaluationStatuses));
            }

            // 5. Filter theo đai (Belt) của học viên (IN)
            if (belts != null && !belts.isEmpty()) {
                predicates.add(studentJoin.get("belt").in(belts));
            }

            // 6. Filter theo chi nhánh (IN)
            if (branchIds != null && !branchIds.isEmpty()) {
                predicates.add(branchJoin.get("branchId").in(branchIds));
            }

            // 7. Filter theo cấp độ lớp (IN)
            if (scheduleLevels != null && !scheduleLevels.isEmpty()) {
                predicates.add(scheduleJoin.get("scheduleStatus").in(scheduleLevels));
            }

            // 8. Filter theo mã lớp học cụ thể
            if (scheduleId != null && !scheduleId.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(scheduleJoin.get("scheduleId"), scheduleId.trim()));
            }

            log.debug("Built {} predicates for StudentAttendance filtering", predicates.size());

            // Kết hợp tất cả các điều kiện bằng AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification riêng: Tìm kiếm theo text (fullName, studentCode, phoneNumber)
     */
    public static Specification<StudentAttendance> searchByText(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // Trả về TRUE (không filter gì)
            }

            Join<StudentAttendance, StudentEnrollment> enrollmentJoin = root.join("studentEnrollment", JoinType.INNER);
            Join<StudentEnrollment, Student> studentJoin = enrollmentJoin.join("student", JoinType.INNER);

            String searchPattern = "%" + search.trim().toLowerCase() + "%";

            Predicate nameMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(studentJoin.get("fullName")),
                    searchPattern
            );
            Predicate codeMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(studentJoin.get("studentCode")),
                    searchPattern
            );
            Predicate phoneMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(studentJoin.get("phoneNumber")),
                    searchPattern
            );

            return criteriaBuilder.or(nameMatch, codeMatch, phoneMatch);
        };
    }

    /**
     * Specification riêng: Filter theo ngày học
     */
    public static Specification<StudentAttendance> hasSessionDate(LocalDate sessionDate) {
        return (root, query, criteriaBuilder) -> {
            if (sessionDate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("sessionDate"), sessionDate);
        };
    }

    /**
     * Specification riêng: Filter theo trạng thái điểm danh
     */
    public static Specification<StudentAttendance> hasAttendanceStatusIn(List<AttendanceStatus> statuses) {
        return (root, query, criteriaBuilder) -> {
            if (statuses == null || statuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("attendanceStatus").in(statuses);
        };
    }

    /**
     * Specification riêng: Filter theo trạng thái đánh giá
     */
    public static Specification<StudentAttendance> hasEvaluationStatusIn(List<EvaluationStatus> statuses) {
        return (root, query, criteriaBuilder) -> {
            if (statuses == null || statuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("evaluationStatus").in(statuses);
        };
    }

    /**
     * Specification riêng: Filter theo đai của học viên
     */
    public static Specification<StudentAttendance> hasBeltIn(List<Belt> belts) {
        return (root, query, criteriaBuilder) -> {
            if (belts == null || belts.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Join<StudentAttendance, StudentEnrollment> enrollmentJoin = root.join("studentEnrollment", JoinType.INNER);
            Join<StudentEnrollment, Student> studentJoin = enrollmentJoin.join("student", JoinType.INNER);
            return studentJoin.get("belt").in(belts);
        };
    }

    /**
     * Specification riêng: Filter theo chi nhánh
     */
    public static Specification<StudentAttendance> hasBranchIdIn(List<Integer> branchIds) {
        return (root, query, criteriaBuilder) -> {
            if (branchIds == null || branchIds.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Join<StudentAttendance, StudentEnrollment> enrollmentJoin = root.join("studentEnrollment", JoinType.INNER);
            Join<StudentEnrollment, ClassSchedule> scheduleJoin = enrollmentJoin.join("classSchedule", JoinType.INNER);
            Join<ClassSchedule, Branch> branchJoin = scheduleJoin.join("branch", JoinType.INNER);
            return branchJoin.get("branchId").in(branchIds);
        };
    }

    /**
     * Specification riêng: Filter theo cấp độ lớp
     */
    public static Specification<StudentAttendance> hasScheduleLevelIn(List<ScheduleLevel> levels) {
        return (root, query, criteriaBuilder) -> {
            if (levels == null || levels.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Join<StudentAttendance, StudentEnrollment> enrollmentJoin = root.join("studentEnrollment", JoinType.INNER);
            Join<StudentEnrollment, ClassSchedule> scheduleJoin = enrollmentJoin.join("classSchedule", JoinType.INNER);
            return scheduleJoin.get("scheduleStatus").in(levels);
        };
    }

    /**
     * Specification riêng: Filter theo mã lớp học
     */
    public static Specification<StudentAttendance> hasScheduleId(String scheduleId) {
        return (root, query, criteriaBuilder) -> {
            if (scheduleId == null || scheduleId.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Join<StudentAttendance, StudentEnrollment> enrollmentJoin = root.join("studentEnrollment", JoinType.INNER);
            Join<StudentEnrollment, ClassSchedule> scheduleJoin = enrollmentJoin.join("classSchedule", JoinType.INNER);
            return criteriaBuilder.equal(scheduleJoin.get("scheduleId"), scheduleId.trim());
        };
    }

    /**
     * Cách sử dụng (Fluent API - Kết hợp các Specification):
     *
     * Example:
     * <pre>
     * Specification<StudentAttendance> spec = Specification.where(
     *     StudentAttendanceSpecification.searchByText(search))
     *     .and(StudentAttendanceSpecification.hasSessionDate(sessionDate))
     *     .and(StudentAttendanceSpecification.hasAttendanceStatusIn(attendanceStatuses))
     *     .and(StudentAttendanceSpecification.hasBeltIn(belts))
     *     // ... thêm các điều kiện khác
     * );
     *
     * Page<StudentAttendance> results = repository.findAll(spec, pageable);
     * </pre>
     */
}


