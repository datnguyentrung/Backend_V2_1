package com.dat.backend_v2_1.repository.Core;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.enums.Core.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
            AND (:scheduleIds IS NULL OR cs.scheduleId IN :scheduleIds)
            ORDER BY cs.scheduleId
            """)
    List<ClassSchedule> findAllWithFilters(
            @Param("branchId") Long branchId,
            @Param("weekday") Weekday weekday,
            @Param("level") ScheduleLevel level,
            @Param("shift") ScheduleShift shift,
            @Param("location") ScheduleLocation location,
            @Param("status") ScheduleStatus status,
            @Param("scheduleIds") List<String> scheduleIds
    );

    List<ClassSchedule> findByWeekdayAndScheduleStatus(Weekday weekday, ScheduleStatus scheduleStatus);

    @Query("SELECT cs FROM ClassSchedule cs " +
            "WHERE cs.weekday = :weekday " +
            "AND cs.scheduleStatus = :status " +
            "AND NOT EXISTS (" +
            "   SELECT 1 FROM ClassSession sess " +
            "   WHERE sess.classSchedule = cs AND sess.sessionDate = :today" +
            ")")
    List<ClassSchedule> findSchedulesNeedingSession(
            @Param("weekday") Weekday weekday,
            @Param("status") ScheduleStatus status,
            @Param("today") LocalDate today
    );
}
