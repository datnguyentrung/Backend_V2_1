package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID> {
    @Modifying
    @Query("""
        update ClassSession c
           set c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.ACTIVE
         where c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.SCHEDULED
           and c.sessionDate = :sessionDate
           and c.startTime <= :thresholdTime
    """)
    int activateScheduledSessions(
            @Param("sessionDate") LocalDate sessionDate,
            @Param("thresholdTime") LocalTime thresholdTime
    );

    @Query("""
        select c
        from ClassSession c
        where c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.ACTIVE
          and c.attendanceClosed = true
          and (
              c.sessionDate < :thresholdDate
              or (c.sessionDate = :thresholdDate and c.endTime <= :thresholdTime)
          )
    """)
    List<ClassSession> findSessionsToComplete(
            @Param("thresholdDate") LocalDate thresholdDate,
            @Param("thresholdTime") LocalTime thresholdTime
    );

    @Modifying
    @Query("""
        update ClassSession c
           set c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.COMPLETED
         where c.classSessionId = :classSessionId
           and c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.ACTIVE
    """)
    int markSessionCompleted(@Param("classSessionId") UUID classSessionId);

    @Query("""
        select c
        from ClassSession c
        where c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.ACTIVE
          and c.attendanceClosed = false
          and c.sessionDate <= :sessionDate
    """)
    List<ClassSession> findSessionsToClose(@Param("sessionDate") LocalDate sessionDate);

    @Query("""
        select c
        from ClassSession c
        where c.course.courseId = :courseId
          and c.sessionDate >= :fromDate
          and c.status in (
              com.dat.ai_receptionist_web.enums.Training.SessionStatus.SCHEDULED,
              com.dat.ai_receptionist_web.enums.Training.SessionStatus.POSTPONED
          )
          and (c.sessionDate > :today or c.endTime > :nowTime)
    """)
    List<ClassSession> findUpcomingSessionsToCancel(
            @Param("courseId") UUID courseId,
            @Param("fromDate") LocalDate fromDate,
            @Param("today") LocalDate today,
            @Param("nowTime") LocalTime nowTime
    );

    @Query("""
        select c.sessionDate
        from ClassSession c
        where c.course.courseId = :courseId
          and c.sessionDate between :fromDate and :untilDate
          and c.status <> :excluded
    """)
    List<LocalDate> findSessionDatesByCourseAndRange(
            @Param("courseId") UUID courseId,
            @Param("fromDate") LocalDate fromDate,
            @Param("untilDate") LocalDate untilDate,
            @Param("excluded") SessionStatus excluded
    );

    boolean existsByCourse_CourseIdAndSessionDateAndStatusNot(
            UUID courseId, LocalDate sessionDate, SessionStatus status);

    List<ClassSession> findByCourse_CourseIdAndSessionDateBetweenAndStatusNot(
            UUID courseId, LocalDate fromDate, LocalDate untilDate, SessionStatus status);
}
