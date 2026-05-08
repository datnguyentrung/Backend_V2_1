package com.dat.backend_v2_1.dto.Core;

import com.dat.backend_v2_1.enums.Skill.SkillLevel;
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