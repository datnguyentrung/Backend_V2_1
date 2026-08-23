package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {
    long countByCoursePurchase_CoursePrice_Course_CourseId(UUID courseId);
    Optional<StudentEnrollment> findByCoursePurchase_CoursePurchaseId(UUID coursePurchaseId);
}
