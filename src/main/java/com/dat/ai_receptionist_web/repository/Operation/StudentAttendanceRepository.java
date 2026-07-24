package com.dat.ai_receptionist_web.repository.Operation;

import com.dat.ai_receptionist_web.domain.Operation.StudentAttendance;
import com.dat.ai_receptionist_web.dto.Operation.AttendanceNotificationRow;
import com.dat.ai_receptionist_web.dto.Operation.CompletedSessionAttendanceNotificationRow;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
            SELECT se.student_id
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
            SELECT se.student_id
            FROM operation.student_attendance sa
            INNER JOIN operation.student_enrollment se
                ON sa.student_enrollment_id = se.enrollment_id
            WHERE sa.class_session_id = :classSessionId
            """, nativeQuery = true)
    List<UUID> findStudentIdsByClassSessionId(@Param("classSessionId") UUID classSessionId);

    @EntityGraph(value = "StudentAttendance.withDetails", type = EntityGraph.EntityGraphType.LOAD)
    Optional<StudentAttendance> findWithDetailsByAttendanceId(UUID attendanceId);

    @Query("""
            SELECT sa.attendanceId AS attendanceId,
                   student.personId AS studentPersonId,
                   student.fullName AS studentName,
                   sa.attendanceStatus AS attendanceStatus,
                   sa.checkInTime AS checkInTime,
                   sa.createdAt AS createdAt,
                   schedule.scheduleId AS scheduleId,
                   coach.fullName AS coachName
            FROM StudentAttendance sa
            JOIN sa.studentEnrollment enrollment
            JOIN enrollment.student student
            JOIN enrollment.classSchedule schedule
            LEFT JOIN sa.recordedByCoach coach
            WHERE sa.attendanceId = :attendanceId
            """)
    Optional<AttendanceNotificationRow> findAttendanceNotificationRow(@Param("attendanceId") UUID attendanceId);

    @Query("""
            SELECT sa.attendanceId AS attendanceId,
                   session.sessionId AS sessionId,
                   student.personId AS studentPersonId,
                   student.fullName AS studentName,
                   sa.attendanceStatus AS attendanceStatus,
                   sa.checkInTime AS checkInTime,
                   sa.createdAt AS createdAt,
                   sa.evaluationStatus AS evaluationStatus,
                   sa.note AS note,
                   schedule.scheduleId AS scheduleId,
                   sa.sessionDate AS sessionDate
            FROM StudentAttendance sa
            JOIN sa.classSession session
            JOIN sa.studentEnrollment enrollment
            JOIN enrollment.student student
            JOIN enrollment.classSchedule schedule
            WHERE session.sessionId = :sessionId
            ORDER BY student.fullName
            """)
    List<CompletedSessionAttendanceNotificationRow> findCompletedSessionAttendanceNotificationRows(
            @Param("sessionId") UUID sessionId
    );

    List<StudentAttendance> findByStudentEnrollment_Student_PersonIdAndSessionDate(UUID studentEnrollmentStudentUserId, LocalDate sessionDate);

    boolean existsByStudentEnrollment_EnrollmentIdAndClassSession_SessionId(UUID enrollmentId, UUID sessionId);

    Optional<StudentAttendance> findByStudentEnrollment_EnrollmentIdAndClassSession_SessionId(
            UUID enrollmentId,
            UUID sessionId
    );

    @Query("""
            SELECT sa.classSession.sessionId
            FROM StudentAttendance sa
            WHERE sa.studentEnrollment.enrollmentId = :enrollmentId
              AND sa.classSession.sessionId IN :sessionIds
            """)
    Set<UUID> findAttendedSessionIds(
            @Param("enrollmentId") UUID enrollmentId,
            @Param("sessionIds") List<UUID> sessionIds
    );
}
