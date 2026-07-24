package com.dat.ai_receptionist_web.dto.Core;

import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FitnessId implements Serializable {
    private Integer fitnessLevel;
    private SkillLevel skillLevel;
}