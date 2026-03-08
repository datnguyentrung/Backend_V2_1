package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.StudentEnrollment;
import com.dat.backend_v2_1.enums.Operation.StudentEnrollmentStatus;
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
    boolean existsByStudent_UserIdAndClassSchedule_ScheduleIdAndStatus(
            UUID student_userId, String classSchedule_scheduleId, @NotNull StudentEnrollmentStatus status
    );

    // Fix N+1 problem: Use JOIN FETCH to eagerly load classSchedule and branch
    @Query("SELECT se FROM StudentEnrollment se " +
            "JOIN FETCH se.classSchedule cs " +
            "JOIN FETCH cs.branch " +
            "WHERE se.student.userId = :userId AND se.status = :status")
    List<StudentEnrollment> findByStudent_UserIdAndStatusWithClassSchedule(
            @Param("userId") UUID userId,
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

    /**
     * Optimized query to fetch active students by schedule ID with their full details.
     * Uses EntityGraph to eagerly fetch Student entity, avoiding N+1 query problem.
     */
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
                WHERE se.student.userId IN :userIds
                AND se.status = :status
            """)
    List<StudentEnrollment> findByStudent_UserIdsInAndStatusWithClassSchedule(
            @Param("userIds") List<UUID> userIds,
            @Param("status") StudentEnrollmentStatus status
    );

    Optional<StudentEnrollment> findByStudent_UserIdAndClassSchedule_ScheduleIdAndStatus(UUID studentUserId, String classScheduleScheduleId, StudentEnrollmentStatus status);

    /**
     * Đếm số học viên trong một lớp theo trạng thái
     */
    long countByClassSchedule_ScheduleIdAndStatus(String scheduleId, StudentEnrollmentStatus status);
}
