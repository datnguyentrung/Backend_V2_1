package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.StudentAttendance;
import com.dat.backend_v2_1.enums.Core.Belt;
import com.dat.backend_v2_1.enums.Core.ScheduleLevel;
import com.dat.backend_v2_1.enums.Operation.AttendanceStatus;
import com.dat.backend_v2_1.enums.Operation.EvaluationStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, UUID>,
        JpaSpecificationExecutor<StudentAttendance>,
        StudentAttendanceRepositoryCustom {

    /**
     * Optimized query using EntityGraph to eagerly fetch related entities.
     * This approach is better than JOIN FETCH because:
     * 1. Avoids cartesian product when fetching multiple collections
     * 2. Uses subselect strategy for better performance
     * 3. Cleaner separation of concerns
     */
    @EntityGraph(attributePaths = {
            "studentEnrollment.student",
            "studentEnrollment.classSchedule",
            "recordedByCoach",
            "evaluatedByCoach"
    })
    @Query("""
            SELECT DISTINCT sa FROM StudentAttendance sa
            WHERE sa.studentEnrollment.classSchedule.scheduleId = :scheduleId
            AND sa.sessionDate = :sessionDate
            ORDER BY sa.studentEnrollment.student.fullName
            """)
    List<StudentAttendance> findByScheduleIdAndSessionDateWithDetails(
            @Param("scheduleId") String scheduleId,
            @Param("sessionDate") LocalDate sessionDate
    );

    @Query(value = """
            SELECT se.student_user_id
            FROM operation.student_attendance sa
            INNER JOIN operation.student_enrollment se
                ON sa.student_enrollment_id = se.enrollment_id
            WHERE se.schedule_id = :scheduleId
            AND sa.session_date = :sessionDate
            """, nativeQuery = true)
    List<UUID> findStudentIdsByScheduleAndSessionDate(
            @Param("scheduleId") @NotNull(message = "Schedule ID không được để trống") String classScheduleId,
            @Param("sessionDate") @NotNull(message = "Ngày học không được để trống") LocalDate sessionDate
    );

    /**
     * ❌ REMOVED: Custom method findAllWithDetails() không hoạt động với Specification
     *
     * Lý do:
     * - Spring Data JPA không hỗ trợ @EntityGraph với custom method name + Specification
     * - Gây lỗi: "No property 'findAllWithDetails' found for type 'StudentAttendance'"
     *
     * ✅ GIẢI PHÁP: Sử dụng method có sẵn từ JpaSpecificationExecutor
     * <pre>
     * // Cách sử dụng ĐÚNG:
     * Specification<StudentAttendance> spec = StudentAttendanceSpecification.filterBy(...);
     * Page<StudentAttendance> results = repository.findAll(spec, pageable);
     *
     * // EntityGraph sẽ được apply thông qua:
     * // 1. Named EntityGraph định nghĩa trong Entity (@NamedEntityGraph)
     * // 2. Fetch JOIN trong Specification (nếu cần thiết)
     * // 3. Query hint trong Custom Repository Implementation
     * </pre>
     *
     * Note: JpaSpecificationExecutor đã cung cấp sẵn method findAll(Specification, Pageable)
     */

    /**
     * @deprecated Sử dụng Specification-based approach thay thế (StudentAttendanceSpecification)
     * <p>
     * Lý do deprecate:
     * 1. JPQL cứng gây lỗi PostgreSQL "could not determine data type" với NULL parameters
     * 2. Khó maintain khi có nhiều điều kiện filter
     * 3. Vi phạm nguyên tắc Clean Code (quá dài, phức tạp)
     * 4. Không type-safe
     * <p>
     * Cách migrate:
     * <pre>
     * // Old way:
     * Page<StudentAttendance> results = repository.findStudentAttendancesWithFilter(
     *     search, sessionDate, attendanceStatuses, evaluationStatuses,
     *     belts, branchIds, scheduleLevels, scheduleId, pageable
     * );
     *
     * // New way:
     * Specification<StudentAttendance> spec = StudentAttendanceSpecification.filterBy(
     *     search, sessionDate, attendanceStatuses, evaluationStatuses,
     *     belts, branchIds, scheduleLevels, scheduleId
     * );
     * Page<StudentAttendance> results = repository.findAll(spec, pageable);
     * </pre>
     */
    @Deprecated(since = "2026-03-08", forRemoval = true)

    @Query(value = """
            SELECT sa
            FROM StudentAttendance sa
            JOIN FETCH sa.studentEnrollment se
            JOIN FETCH se.student s
            JOIN FETCH se.classSchedule cs
            JOIN FETCH cs.branch b
            WHERE (LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(s.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))
                AND (:sessionDate IS NULL OR sa.sessionDate = :sessionDate)
                AND (:attendanceStatuses IS NULL OR sa.attendanceStatus IN :attendanceStatuses)
                AND (:evaluationStatuses IS NULL OR sa.evaluationStatus IN :evaluationStatuses)
                AND (:belts IS NULL OR s.belt IN :belts)
                AND (:branchIds IS NULL OR b.branchId IN :branchIds)
                AND (:scheduleLevels IS NULL OR cs.scheduleStatus IN :scheduleLevels)
                AND (:scheduleId IS NULL OR cs.scheduleId = :scheduleId)""",
            countQuery = """
                    SELECT COUNT(sa.attendanceId)
                    FROM StudentAttendance sa
                    JOIN sa.studentEnrollment se
                    JOIN se.student s
                    JOIN se.classSchedule cs
                    JOIN cs.branch b
                    WHERE (LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(s.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))
                        AND (:sessionDate IS NULL OR sa.sessionDate = :sessionDate)
                        AND (:attendanceStatuses IS NULL OR sa.attendanceStatus IN :attendanceStatuses)
                        AND (:evaluationStatuses IS NULL OR sa.evaluationStatus IN :evaluationStatuses)
                        AND (:belts IS NULL OR s.belt IN :belts)
                        AND (:branchIds IS NULL OR b.branchId IN :branchIds)
                        AND (:scheduleLevels IS NULL OR cs.scheduleStatus IN :scheduleLevels)
                        AND (:scheduleId IS NULL OR cs.scheduleId = :scheduleId)""")
    Page<StudentAttendance> findStudentAttendancesWithFilter(
            @Param("search") String search,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("attendanceStatuses") List<AttendanceStatus> attendanceStatuses,
            @Param("evaluationStatuses") List<EvaluationStatus> evaluationStatuses,
            @Param("belts") List<Belt> belts,
            @Param("branchIds") List<Integer> branchIds,
            @Param("scheduleLevels") List<ScheduleLevel> levels,
            @Param("scheduleId") String scheduleId,
            Pageable pageable
    );

    List<StudentAttendance> findByStudentEnrollment_Student_UserIdAndSessionDate(UUID studentEnrollmentStudentUserId, LocalDate sessionDate);
}
