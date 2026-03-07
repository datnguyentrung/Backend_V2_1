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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, UUID> {

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
                AND (:scheduleLevels IS NULL OR cs.scheduleStatus IN :scheduleLevels)""",
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
                        AND (:scheduleLevels IS NULL OR cs.scheduleStatus IN :scheduleLevels)""")
    Page<StudentAttendance> findStudentAttendancesWithFilter(
            @Param("search") String search,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("attendanceStatuses") List<AttendanceStatus> attendanceStatuses,
            @Param("evaluationStatuses") List<EvaluationStatus> evaluationStatuses,
            @Param("belts") List<Belt> belts,
            @Param("branchIds") List<Integer> branchIds,
            @Param("scheduleLevels") List<ScheduleLevel> levels,
            Pageable pageable
    );
}
