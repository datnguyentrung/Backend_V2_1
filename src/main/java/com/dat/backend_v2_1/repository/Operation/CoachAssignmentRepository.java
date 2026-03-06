package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.CoachAssignment;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CoachAssignmentRepository extends JpaRepository<CoachAssignment, UUID> {

    @Query("""
        SELECT ca FROM CoachAssignment ca
        WHERE ca.coach.userId = :coachId
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
        WHERE ca.coach.userId = :coachId AND ca.status = :status
    """)
    List<CoachAssignment> findByCoach_UserIdAndStatusWithClassSchedule(
            @Param("coachId") UUID coachId,
            @Param("status") CoachAssignmentStatus status
    );

    List<CoachAssignment> findByStatus(CoachAssignmentStatus status);

    /**
     * Tìm CoachAssignment theo scheduleId và status
     */
    List<CoachAssignment> findByClassSchedule_ScheduleIdAndStatus(String scheduleId, CoachAssignmentStatus status);

    /**
     * Đếm số coach assignment trong một lớp theo trạng thái
     */
    long countByClassSchedule_ScheduleIdAndStatus(String scheduleId, CoachAssignmentStatus status);
}
