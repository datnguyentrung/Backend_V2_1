package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Operation.CoachAssignment;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CoachAssignmentRepository extends JpaRepository<CoachAssignment, UUID> {

    boolean existsByCoachAndClassSchedule_ScheduleIdAndStatus(@NotNull Coach coach, String classSchedule_scheduleId, @NotNull CoachAssignmentStatus status);

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
}
