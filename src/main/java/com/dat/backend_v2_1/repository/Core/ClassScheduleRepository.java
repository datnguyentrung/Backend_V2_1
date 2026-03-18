package com.dat.backend_v2_1.repository.Core;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.enums.Core.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, String> {

    /**
     * Find all class schedules with optional filters
     * Uses JOIN FETCH to avoid N+1 problem when loading branch
     */
    @Query("""
            SELECT DISTINCT cs FROM ClassSchedule cs
            JOIN FETCH cs.branch b
            WHERE (:branchId IS NULL OR b.branchId = :branchId)
            AND (:weekday IS NULL OR cs.weekday = :weekday)
            AND (:level IS NULL OR cs.level = :level)
            AND (:shift IS NULL OR cs.shift = :shift)
            AND (:location IS NULL OR cs.location = :location)
            AND (:status IS NULL OR cs.scheduleStatus = :status)
            ORDER BY cs.scheduleId
            """)
    List<ClassSchedule> findAllWithFilters(
            @Param("branchId") Long branchId,
            @Param("weekday") Weekday weekday,
            @Param("level") ScheduleLevel level,
            @Param("shift") ScheduleShift shift,
            @Param("location") ScheduleLocation location,
            @Param("status") ScheduleStatus status
    );

    List<ClassSchedule> findByWeekdayAndScheduleStatus(Weekday weekday, ScheduleStatus scheduleStatus);
}
