package com.dat.ai_receptionist_web.mapper.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {
    public CourseDTO.Response toResponse(Course entity) {
        if (entity == null) return null;
        return new CourseDTO.Response(entity.getCourseId(), entity.getClassSchedule() == null ? null : entity.getClassSchedule().getScheduleId(), entity.getCapacity(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public void updateEntity(CourseDTO.UpdateRequest request, Course entity) {
        entity.setCapacity(request.capacity());
        entity.setStatus(request.status());
    }
}
