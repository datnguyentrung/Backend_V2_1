package com.dat.backend_v2_1.repository.Core;

import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.dto.Core.StudentResDTO;
import com.dat.backend_v2_1.dto.Operation.CheckInStudentProjection;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID>,
        JpaSpecificationExecutor<Student>,
        StudentRepositoryCustom {

    @Override
    @EntityGraph(attributePaths = {"branch"})
    Page<Student> findAll(@Nullable Specification<Student> spec, Pageable pageable);

    boolean existsByNationalCode(String nationalCode);

    boolean existsByStudentCode(String generatedCode);

    @Query(value = """
            SELECT DISTINCT s FROM Student s
            LEFT JOIN FETCH s.branch
            LEFT JOIN StudentEnrollment se ON se.student = s
            WHERE (:isFilterSchedule = false OR se.classSchedule.scheduleId IN :scheduleIds)
              AND (LOWER(s.fullName) LIKE :search OR LOWER(s.studentCode) LIKE :search)
              AND (:status IS NULL OR s.studentStatus = :status)
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT s) FROM Student s
                    LEFT JOIN StudentEnrollment se ON se.student = s
                    WHERE (:isFilterSchedule = false OR se.classSchedule.scheduleId IN :scheduleIds)
                      AND (LOWER(s.fullName) LIKE :search OR LOWER(s.studentCode) LIKE :search)
                      AND (:status IS NULL OR s.studentStatus = :status)
                    """)
    Page<Student> findStudentsWithFilter(
            @Param("search") String search,
            @Param("status") StudentStatus status,
            @Param("scheduleIds") List<String> scheduleIds,
            @Param("isFilterSchedule") boolean isFilterSchedule,
            Pageable pageable
    );

    @Query("""
            SELECT s.studentStatus AS status, COUNT(DISTINCT s) AS count
            FROM Student s
            LEFT JOIN StudentEnrollment se ON se.student = s
            WHERE (:isFilterSchedule = false OR se.classSchedule.scheduleId IN :scheduleIds)
              AND (LOWER(s.fullName) LIKE :search OR LOWER(s.studentCode) LIKE :search)
            GROUP BY s.studentStatus
            """)
    List<StudentStatusCount> countStudentsByStatusWithFilter(
            @Param("search") String search,
            @Param("scheduleIds") List<String> scheduleIds,
            @Param("isFilterSchedule") boolean isFilterSchedule
    );

    @Query("""
            SELECT s.studentCode AS studentCode,
                   s.fullName AS fullName,
                   s.belt AS belt
            FROM Student s
            WHERE s.studentCode IN :studentCodes
            """)
    List<StudentResDTO.StudentRankInfo> findRankInfoByStudentCodeIn(@Param("studentCodes") List<String> studentCodes);

    List<Student> findAllByStudentStatus(StudentStatus studentStatus);

    interface StudentStatusCount {
        StudentStatus getStatus();

        Long getCount();
    }

    @Query("""
            SELECT s.studentStatus, COUNT(s)
            FROM Student s
            GROUP BY s.studentStatus
            """)
    List<Object[]> countStudentsByStatusGrouped();

    Optional<Student> findByStudentCode(String studentCode);

    @Query("""
            SELECT s.personId AS personId,
                   s.studentCode AS studentCode,
                   s.studentStatus AS studentStatus,
                   s.fullName AS fullName
            FROM Student s
            WHERE s.studentCode = :studentCode
            """)
    Optional<CheckInStudentProjection> findCheckInStudentByStudentCode(@Param("studentCode") String studentCode);
}
