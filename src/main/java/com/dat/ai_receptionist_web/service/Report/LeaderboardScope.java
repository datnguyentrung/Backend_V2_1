package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;

import java.util.Objects;

public record LeaderboardScope(Type type, int year, int quarter, SkillLevel skillLevel) {
    public enum Type {
        QUARTER,
        FITNESS
    }

    public LeaderboardScope {
        if (quarter < 1 || quarter > 4) {
            throw new IllegalArgumentException("Quarter must be between 1 and 4");
        }
        if (type == Type.FITNESS) {
            Objects.requireNonNull(skillLevel, "Skill level is required for fitness leaderboard");
        }
    }

    public static LeaderboardScope quarter(int year, int quarter) {
        return new LeaderboardScope(Type.QUARTER, year, quarter, null);
    }

    public static LeaderboardScope fitness(int year, int quarter, SkillLevel skillLevel) {
        return new LeaderboardScope(Type.FITNESS, year, quarter, skillLevel);
    }

    public String rankKey() {
        return type == Type.QUARTER
                ? "leaderboard:%d:Q%d".formatted(year, quarter)
                : "leaderboard:%d:Q%d:fitness:%s".formatted(year, quarter, skillLevel);
    }

    public String dataKey() {
        return type == Type.QUARTER
                ? "leaderboard_data:%d:Q%d".formatted(year, quarter)
                : "leaderboard_data:%d:Q%d:fitness:%s".formatted(year, quarter, skillLevel);
    }

    public String memberKey() {
        return type == Type.QUARTER
                ? "leaderboard_member:%d:Q%d".formatted(year, quarter)
                : "leaderboard_member:%d:Q%d:fitness:%s".formatted(year, quarter, skillLevel);
    }

    public String historyKey() {
        return "leaderboard_history:%d:Q%d:fitness:%s".formatted(year, quarter, skillLevel);
    }

    public String stateKey() {
        return type == Type.QUARTER
                ? "leaderboard_state:%d:Q%d".formatted(year, quarter)
                : "leaderboard_state:%d:Q%d:fitness:%s".formatted(year, quarter, skillLevel);
    }

    public String registryValue() {
        return type == Type.QUARTER
                ? "quarter|%d|%d".formatted(year, quarter)
                : "fitness|%d|%d|%s".formatted(year, quarter, skillLevel);
    }

    public static LeaderboardScope fromRegistryValue(String value) {
        String[] parts = value.split("\\|");
        if (parts.length == 3 && "quarter".equals(parts[0])) {
            return quarter(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
        if (parts.length == 4 && "fitness".equals(parts[0])) {
            return fitness(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), SkillLevel.valueOf(parts[3]));
        }
        throw new IllegalArgumentException("Invalid leaderboard scope: " + value);
    }
}
