package com.dat.ai_receptionist_web.repository.Operation;

import com.dat.ai_receptionist_web.domain.Operation.CoachTimesheet;
import com.dat.ai_receptionist_web.dto.Operation.CoachTimesheetStatusProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoachTimesheetRepository extends JpaRepository<CoachTimesheet, UUID>,
        JpaSpecificationExecutor<CoachTimesheet>,
        CoachTimesheetRepositoryCustom {

    boolean existsByCoachAssignment_AssignmentIdAndClassSession_SessionId(UUID coachAssignmentId, UUID classSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(value = "CoachTimesheet.withDetails")
    Optional<CoachTimesheet> findForCheckInByCoachAssignment_AssignmentIdAndClassSession_SessionId(
            UUID coachAssignmentId,
            UUID classSessionId
    );

    @EntityGraph(value = "CoachTimesheet.withDetails")
    Optional<CoachTimesheet> findWithDetailsByTimesheetId(UUID timesheetId);

    @Modifying
    @Query("DELETE FROM CoachTimesheet ct WHERE ct.timesheetId = :timesheetId")
    int deleteByTimesheetId(@Param("timesheetId") UUID timesheetId);

    @EntityGraph(value = "CoachTimesheet.withDetails")
    List<CoachTimesheet> findByCoach_PersonIdAndWorkingDateBetween(
            UUID coachId,
            LocalDate fromDate,
            LocalDate toDate
    );

    @Query("""
            SELECT ct FROM CoachTimesheet ct
            JOIN FETCH ct.coach coach
            JOIN FETCH ct.classSession session
            JOIN FETCH session.classSchedule cs
            LEFT JOIN FETCH cs.branch
            WHERE coach.personId = :coachId
            AND ct.workingDate >= :fromDate
            AND ct.workingDate <= :toDate
            """)
    List<CoachTimesheet> findByCoachAndDateRangeWithDetails(
            @Param("coachId") UUID coachId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
            SELECT ct.coach.personId AS coachId,
                   ct.status AS status,
                   ct.checkInTime AS checkInTime,
                   ct.checkOutTime AS checkOutTime
            FROM CoachTimesheet ct
            WHERE ct.classSession.sessionId = :classSessionId
            """)
    List<CoachTimesheetStatusProjection> findStatusByClassSessionId(
            @Param("classSessionId") UUID classSessionId
    );
}
