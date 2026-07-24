package com.dat.ai_receptionist_web.repository.Operation;

import com.dat.ai_receptionist_web.domain.Operation.StudentEnrollment;
import com.dat.ai_receptionist_web.enums.Operation.StudentEnrollmentStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {
    boolean existsByStudent_PersonIdAndClassSchedule_ScheduleIdAndStatus(
            UUID studentPersonId, String classScheduleScheduleId, @NotNull StudentEnrollmentStatus status
    );

    @Query("SELECT se FROM StudentEnrollment se " +
            "JOIN FETCH se.classSchedule cs " +
            "JOIN FETCH cs.branch " +
            "WHERE se.student.personId = :personId AND se.status = :status")
    List<StudentEnrollment> findByStudent_PersonIdAndStatusWithClassSchedule(
            @Param("personId") UUID personId,
            @Param("status") StudentEnrollmentStatus status
    );

    @Query("""
            SELECT se.classSchedule.scheduleId FROM StudentEnrollment se
            WHERE se.student.personId = :personId
            AND se.status = :status
            AND se.classSchedule.scheduleId IN :scheduleIds
            """)
    List<String> findActiveScheduleIdsByStudentPersonIdAndScheduleIds(
            @Param("personId") UUID personId,
            @Param("scheduleIds") List<String> scheduleIds,
            @Param("status") StudentEnrollmentStatus status
    );

    @Query("SELECT se FROM StudentEnrollment se " +
            "JOIN FETCH se.classSchedule cs " +
            "JOIN FETCH cs.branch " +
            "WHERE se.student.studentCode = :studentCode AND se.status = :status")
    List<StudentEnrollment> findByStudent_StudentCodeAndStatusWithClassSchedule(
            @Param("studentCode") String studentCode,
            @Param("status") StudentEnrollmentStatus status
    );

    @EntityGraph(attributePaths = {"student"})
    @Query("""
            SELECT se FROM StudentEnrollment se
            WHERE se.classSchedule.scheduleId = :scheduleId
            AND se.status = :status
            ORDER BY se.student.fullName
            """)
    List<StudentEnrollment> findByScheduleIdAndStatusWithStudent(
            @Param("scheduleId") String scheduleId,
            @Param("status") StudentEnrollmentStatus status
    );

    @Query("""
            SELECT se FROM StudentEnrollment se
            JOIN FETCH se.classSchedule cs
            LEFT JOIN FETCH cs.branch
            WHERE se.student.personId IN :personIds
            AND se.status = :status
            """)
    List<StudentEnrollment> findByStudent_PersonIdsInAndStatusWithClassSchedule(
            @Param("personIds") List<UUID> personIds,
            @Param("status") StudentEnrollmentStatus status
    );

    Optional<StudentEnrollment> findByStudent_PersonIdAndClassSchedule_ScheduleIdAndStatus(
            UUID studentPersonId,
            String classScheduleScheduleId,
            StudentEnrollmentStatus status
    );

    long countByClassSchedule_ScheduleIdAndStatus(String scheduleId, StudentEnrollmentStatus status);
}
