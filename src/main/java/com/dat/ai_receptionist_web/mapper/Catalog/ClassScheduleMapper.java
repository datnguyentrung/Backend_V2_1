package com.dat.ai_receptionist_web.mapper.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import com.dat.ai_receptionist_web.dto.Catalog.ClassScheduleDTO;
import org.springframework.stereotype.Component;

@Component
public class ClassScheduleMapper {
    public ClassScheduleDTO.Response toResponse(ClassSchedule entity) {
        if (entity == null) return null;
        return new ClassScheduleDTO.Response(entity.getScheduleId(), entity.getBranch() == null ? null : entity.getBranch().getBranchId(), entity.getWeekday(), entity.getLevel(), entity.getLocation(), entity.getStatus(), entity.getStartTime(), entity.getEndTime());
    }

    public void updateEntity(ClassScheduleDTO.UpdateRequest request, ClassSchedule entity) {
        entity.setWeekday(request.weekday());
        entity.setLevel(request.level());
        entity.setLocation(request.location());
        entity.setStatus(request.status());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
    }
}
