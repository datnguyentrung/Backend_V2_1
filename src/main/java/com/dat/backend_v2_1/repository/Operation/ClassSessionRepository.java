package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.ClassSession;
import com.dat.backend_v2_1.dto.Operation.ClassSessionReportSessionRow;
import com.dat.backend_v2_1.enums.Operation.SessionStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID>, JpaSpecificationExecutor<ClassSession> {
    @Query("SELECT cs FROM ClassSession cs " +
            "WHERE cs.isAttendanceClosed = false " +
            "AND cs.status IN ('ACTIVE', 'COMPLETED', 'TERMINATED') " +
            "AND cs.sessionDate = :thresholdDate " +
            "AND cs.startTime <= :thresholdTime")
    List<ClassSession> findClassSessionToClose(
            @Param("thresholdDate") LocalDate thresholdDate,
            @Param("thresholdTime") LocalTime thresholdTime
    );

    @Modifying
    @Query("UPDATE ClassSession cs SET cs.status = 'ACTIVE' " +
            "WHERE cs.status = 'SCHEDULED' " +
            "AND cs.sessionDate = :currentDate " +
            "AND cs.startTime <= :currentTime")
    int activateScheduledSessions(@Param("currentDate") LocalDate currentDate,
                                  @Param("currentTime") LocalTime currentTime);

    @Modifying
    @Query("UPDATE ClassSession cs SET cs.status = 'COMPLETED' " +
            "WHERE cs.status = 'ACTIVE' " +
            "AND cs.sessionDate = :currentDate " +
            "AND cs.endTime <= :currentTime")
    int completeScheduledSessions(@Param("currentDate") LocalDate currentDate,
                                  @Param("currentTime") LocalTime currentTime);

    @Query("""
    SELECT cs
    FROM ClassSession cs
    WHERE cs.isAttendanceClosed = false
      AND cs.endTime IS NOT NULL
      AND cs.status IN ('ACTIVE', 'COMPLETED', 'TERMINATED')
      AND (
          cs.sessionDate < :thresholdDate
          OR (
              cs.sessionDate = :thresholdDate
              AND cs.endTime <= :thresholdTime
          )
      )
    ORDER BY cs.sessionDate ASC, cs.endTime ASC
""")
    List<ClassSession> findClassSessionsToCloseAttendance(
            @Param("thresholdDate") LocalDate thresholdDate,
            @Param("thresholdTime") LocalTime thresholdTime
    );

    @Query("""
        SELECT cs
        FROM ClassSession cs
        JOIN FETCH cs.classSchedule schedule
        WHERE cs.status = 'ACTIVE'
          AND cs.isAttendanceClosed = true
          AND cs.endTime IS NOT NULL
          AND (
              cs.sessionDate < :thresholdDate
              OR (
                  cs.sessionDate = :thresholdDate
                  AND cs.endTime <= :thresholdTime
              )
          )
        ORDER BY cs.sessionDate ASC, cs.endTime ASC
    """)
    List<ClassSession> findClassSessionsToComplete(
            @Param("thresholdDate") LocalDate thresholdDate,
            @Param("thresholdTime") LocalTime thresholdTime
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE ClassSession cs
    SET cs.status = 'COMPLETED'
    WHERE cs.sessionId = :sessionId
      AND cs.status = 'ACTIVE'
      AND cs.isAttendanceClosed = true
""")
    int markSessionCompleted(
            @Param("sessionId") UUID sessionId
    );

    List<ClassSession> findBySessionDate(LocalDate sessionDate);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"classSchedule", "classSchedule.branch"})
    Page<ClassSession> findAll(@NonNull Specification<ClassSession> spec, @NonNull Pageable pageable);

    List<ClassSession> findBySessionDateAndClassSchedule_ScheduleIdIn(LocalDate today, List<String> enrolledScheduleIds);

    @EntityGraph(attributePaths = {"classSchedule"})
    List<ClassSession> findBySessionDateAndStatusAndClassSchedule_ScheduleIdIn(
            LocalDate sessionDate,
            SessionStatus status,
            List<String> scheduleIds
    );

    @EntityGraph(attributePaths = {"classSchedule"})
    List<ClassSession> findBySessionDateAndClassSchedule_ScheduleId(LocalDate sessionDate, String scheduleId);

    @EntityGraph(attributePaths = {"classSchedule", "classSchedule.branch"})
    Optional<ClassSession> findFirstBySessionDateAndClassSchedule_ScheduleId(LocalDate sessionDate, String scheduleId);

    @Query("""
            SELECT cs.sessionId AS sessionId,
                   cs.sessionDate AS sessionDate,
                   schedule.scheduleId AS classScheduleId,
                   branch.branchName AS branchName,
                   cs.startTime AS startTime,
                   cs.endTime AS endTime
            FROM ClassSession cs
            JOIN cs.classSchedule schedule
            LEFT JOIN schedule.branch branch
            WHERE cs.sessionId = :sessionId
            """)
    Optional<ClassSessionReportSessionRow> findReportSessionRow(@Param("sessionId") UUID sessionId);
}
