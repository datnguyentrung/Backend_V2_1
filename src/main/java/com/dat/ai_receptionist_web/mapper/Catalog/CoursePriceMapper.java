package com.dat.ai_receptionist_web.mapper.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CoursePrice;
import com.dat.ai_receptionist_web.dto.Catalog.CoursePriceDTO;
import org.springframework.stereotype.Component;

@Component
public class CoursePriceMapper {
    public CoursePriceDTO.Response toResponse(CoursePrice entity) {
        if (entity == null) return null;
        return new CoursePriceDTO.Response(entity.getCoursePriceId(), entity.getCourse() == null ? null : entity.getCourse().getCourseId(), entity.getDurationMonths(), entity.getSessionCount(), entity.getBasePrice(), entity.getFinalPrice(), entity.getStatus());
    }

    public void updateEntity(CoursePriceDTO.UpdateRequest request, CoursePrice entity) {
        entity.setDurationMonths(request.durationMonths());
        entity.setSessionCount(request.sessionCount());
        entity.setBasePrice(request.basePrice());
        entity.setFinalPrice(request.finalPrice());
        entity.setStatus(request.status());
    }
}
