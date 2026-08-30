package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {
    long countByCoursePurchase_CoursePrice_Course_CourseId(UUID courseId);
    Optional<StudentEnrollment> findByCoursePurchase_CoursePurchaseId(UUID coursePurchaseId);

    @Query("""
        select e
        from StudentEnrollment e
        join e.coursePurchase p
        join p.coursePrice pr
        where e.studentPerson.personId = :personId
          and pr.course.courseId = :courseId
          and e.status = com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.ACTIVE
          and e.startDate <= :sessionDate
          and e.endDate >= :sessionDate
    """)
    Optional<StudentEnrollment> findActiveEnrollmentForCourseOnDate(
            @Param("personId") UUID personId,
            @Param("courseId") UUID courseId,
            @Param("sessionDate") LocalDate sessionDate
    );

    @Query("""
        select e
        from StudentEnrollment e
        join e.coursePurchase p
        join p.coursePrice pr
        where pr.course.courseId = :courseId
          and e.status = com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.ACTIVE
          and e.startDate <= :sessionDate
          and e.endDate >= :sessionDate
    """)
    List<StudentEnrollment> findActiveEnrollmentsForCourseOnDate(
            @Param("courseId") UUID courseId,
            @Param("sessionDate") LocalDate sessionDate
    );
}
