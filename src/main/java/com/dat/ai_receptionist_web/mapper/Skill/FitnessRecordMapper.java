package com.dat.ai_receptionist_web.mapper.Skill;

import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import org.springframework.stereotype.Component;

@Component
public class FitnessRecordMapper {
    public FitnessRecordDTO.Response toResponse(FitnessRecord entity) {
        if (entity == null) return null;
        return new FitnessRecordDTO.Response(entity.getFitnessRecordId(), entity.getStudent() == null ? null : entity.getStudent().getPersonId(), entity.getFitness() == null ? null : entity.getFitness().getFitnessId(), entity.getRecordedByCoach() == null ? null : entity.getRecordedByCoach().getPersonId(), entity.getRecordDate(), entity.getDuration(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public void updateEntity(FitnessRecordDTO.UpdateRequest request, FitnessRecord entity) {
        entity.setRecordDate(request.recordDate());
        entity.setDuration(request.duration());
    }
}
