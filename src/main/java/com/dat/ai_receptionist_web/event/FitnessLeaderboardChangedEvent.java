package com.dat.ai_receptionist_web.event;

import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;

import java.util.Set;

public record FitnessLeaderboardChangedEvent(
        String studentCode,
        Set<Scope> affectedScopes
) {
    public record Scope(int year, int quarter, SkillLevel skillLevel) {
    }
}
