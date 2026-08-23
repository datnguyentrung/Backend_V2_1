package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CoursePrice;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface CoursePriceRepository extends JpaRepository<CoursePrice, UUID> {
    @EntityGraph(attributePaths = {"course", "course.classSchedule"})
    @Query("select cp from CoursePrice cp where cp.coursePriceId = :id")
    Optional<CoursePrice> findForPurchase(@Param("id") UUID id);
}
