package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.courseId = :id")
    Optional<Course> findByIdForUpdate(@Param("id") UUID id);
}
