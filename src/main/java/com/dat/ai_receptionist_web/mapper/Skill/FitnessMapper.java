package com.dat.ai_receptionist_web.mapper.Skill;

import com.dat.ai_receptionist_web.domain.Skill.Fitness;
import com.dat.ai_receptionist_web.dto.Skill.FitnessDTO;
import org.springframework.stereotype.Component;

@Component
public class FitnessMapper {
    public FitnessDTO.Response toResponse(Fitness entity) {
        if (entity == null) return null;
        return new FitnessDTO.Response(entity.getFitnessId(), entity.getScheduleLevel(), entity.getAmount(), entity.getDuration());
    }

    public void updateEntity(FitnessDTO.UpdateRequest request, Fitness entity) {
        entity.setScheduleLevel(request.scheduleLevel());
        entity.setAmount(request.amount());
        entity.setDuration(request.duration());
    }
}
