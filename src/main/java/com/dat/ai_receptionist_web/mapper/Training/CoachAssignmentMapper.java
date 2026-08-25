package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.CoachAssignment;
import com.dat.ai_receptionist_web.dto.Training.CoachAssignmentDTO;
import org.springframework.stereotype.Component;

@Component
public class CoachAssignmentMapper {
    public CoachAssignmentDTO.Response toResponse(CoachAssignment entity) {
        if (entity == null) return null;
        return new CoachAssignmentDTO.Response(entity.getCoachAssignmentId(), entity.getCoach() == null ? null : entity.getCoach().getPersonId(), entity.getCourse() == null ? null : entity.getCourse().getCourseId(), entity.getAssignedDate(), entity.getEndDate(), entity.getCoachAssignmentStatus(), entity.getNote(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public void updateEntity(CoachAssignmentDTO.UpdateRequest request, CoachAssignment entity) {
        entity.setAssignedDate(request.assignedDate());
        entity.setEndDate(request.endDate());
        entity.setCoachAssignmentStatus(request.coachAssignmentStatus());
        entity.setNote(request.note());
    }
}
