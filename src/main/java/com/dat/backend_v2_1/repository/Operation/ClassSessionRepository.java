package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID> {
    boolean existsBySessionDate(LocalDate sessionDate);

    @Query("SELECT cs FROM ClassSession cs JOIN FETCH ClassSchedule sch ON cs.classSchedule.scheduleId = sch.scheduleId " +
            "WHERE cs.isAttendanceClosed = false " +
            "AND cs.sessionDate = :thresholdDate " +
            "AND sch.startTime <= :thresholdTime")
    List<ClassSession> findClassSessionToClose(
            @Param("thresholdDate") LocalDate thresholdDate,
            @Param("thresholdTime") LocalTime thresholdTime
    );
}
