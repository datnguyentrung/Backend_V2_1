package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.CoachTimesheet;
import com.dat.ai_receptionist_web.dto.Training.CoachTimesheetDTO;
import org.springframework.stereotype.Component;

@Component
public class CoachTimesheetMapper {
    public CoachTimesheetDTO.Response toResponse(CoachTimesheet entity) {
        if (entity == null) return null;
        return new CoachTimesheetDTO.Response(entity.getCoachTimesheetId(), entity.getCoachAssignment() == null ? null : entity.getCoachAssignment().getCoachAssignmentId(), entity.getClassSession() == null ? null : entity.getClassSession().getClassSessionId(), entity.getCheckInTime(), entity.getCheckOutTime(), entity.getNote(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public void updateEntity(CoachTimesheetDTO.UpdateRequest request, CoachTimesheet entity) {
        entity.setCheckInTime(request.checkInTime());
        entity.setCheckOutTime(request.checkOutTime());
        entity.setNote(request.note());
    }
}
