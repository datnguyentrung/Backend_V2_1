package com.dat.ai_receptionist_web.repository.Operation;

import com.dat.ai_receptionist_web.domain.Operation.CoachAssignment;
import com.dat.ai_receptionist_web.dto.Operation.ResponsibleCoachProjection;
import com.dat.ai_receptionist_web.enums.Core.Weekday;
import com.dat.ai_receptionist_web.enums.Operation.CoachAssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoachAssignmentRepository extends JpaRepository<CoachAssignment, UUID>, JpaSpecificationExecutor<CoachAssignment> {

    @EntityGraph(value = "CoachAssignment.withDetails")
    Page<CoachAssignment> findAll(Specification<CoachAssignment> spec, Pageable pageable);

    @Query("""
            SELECT ca FROM CoachAssignment ca
            WHERE ca.coach.personId = :coachId
            AND ca.classSchedule.scheduleId IN :scheduleIds
            AND ca.status = :status
            """)
    List<CoachAssignment> findByCoachAndScheduleIdsAndStatus(
            @Param("coachId") UUID coachId,
            @Param("scheduleIds") List<String> scheduleIds,
            @Param("status") CoachAssignmentStatus status
    );

    @Query("""
            SELECT ca FROM CoachAssignment ca
            JOIN FETCH ca.classSchedule cs
            JOIN FETCH cs.branch
            WHERE ca.coach.personId = :coachId AND ca.status = :status
            """)
    List<CoachAssignment> findByCoach_PersonIdAndStatusWithClassSchedule(
            @Param("coachId") UUID coachId,
            @Param("status") CoachAssignmentStatus status
    );

    @Query("""
            SELECT ca FROM CoachAssignment ca
            JOIN FETCH ca.classSchedule cs
            LEFT JOIN FETCH cs.branch
            WHERE ca.coach.personId IN :coachIds AND ca.status = :status
            """)
    List<CoachAssignment> findByCoachIdInAndStatusWithClassSchedule(
            @Param("coachIds") List<UUID> coachIds,
            @Param("status") CoachAssignmentStatus status
    );

    @EntityGraph(value = "CoachAssignment.withDetails")
    Optional<CoachAssignment> findWithDetailsByAssignmentId(UUID assignmentId);

    List<CoachAssignment> findByStatus(CoachAssignmentStatus status);

    List<CoachAssignment> findByClassSchedule_ScheduleIdAndStatus(String scheduleId, CoachAssignmentStatus status);

    long countByClassSchedule_ScheduleIdAndStatus(String scheduleId, CoachAssignmentStatus status);

    @EntityGraph(value = "CoachAssignment.withDetails")
    @Query("""
            SELECT ca FROM CoachAssignment ca
            WHERE ca.coach.personId = :coachId
            AND ca.classSchedule.scheduleId = :scheduleId
            AND ca.status = :status
            AND ca.assignedDate <= :workDate
            AND (ca.endDate IS NULL OR ca.endDate >= :workDate)
            """)
    Optional<CoachAssignment> findValidAssignment(
            @Param("coachId") UUID coachId,
            @Param("scheduleId") String scheduleId,
            @Param("workDate") LocalDate workDate,
            @Param("status") CoachAssignmentStatus status
    );

    @Query("""
            SELECT COUNT(ca) > 0 FROM CoachAssignment ca
            WHERE ca.coach.personId = :coachId
            AND (:excludedId IS NULL OR ca.assignmentId <> :excludedId)
            AND ca.status IN :statuses
            AND ca.classSchedule.weekday = :weekday
            AND ca.classSchedule.startTime < :endTime
            AND ca.classSchedule.endTime > :startTime
            AND ca.assignedDate <= :endDate
            AND (ca.endDate IS NULL OR ca.endDate >= :startDate)
            """)
    boolean existsOverlappingAssignment(
            @Param("coachId") UUID coachId,
            @Param("weekday") Weekday weekday,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<CoachAssignmentStatus> statuses,
            @Param("excludedId") UUID excludedId
    );

    @Query("""
            SELECT ca.assignmentId AS assignmentId,
                   coach.personId AS coachPersonId,
                   coach.fullName AS coachName
            FROM CoachAssignment ca
            JOIN ca.coach coach
            JOIN ca.classSchedule schedule
            WHERE schedule.scheduleId = :scheduleId
              AND ca.status = :status
              AND ca.assignedDate <= :sessionDate
              AND (ca.endDate IS NULL OR ca.endDate >= :sessionDate)
            """)
    List<ResponsibleCoachProjection> findResponsibleCoaches(
            @Param("scheduleId") String scheduleId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("status") CoachAssignmentStatus status
    );
}
