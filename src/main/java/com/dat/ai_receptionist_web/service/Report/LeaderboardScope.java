package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import java.util.Objects;

public record LeaderboardScope(Type type, int year, int quarter, ScheduleLevel scheduleLevel) {
    public enum Type { QUARTER, FITNESS }

    public LeaderboardScope {
        if (quarter < 1 || quarter > 4) throw new IllegalArgumentException("Quarter must be between 1 and 4");
        if (type == Type.FITNESS) Objects.requireNonNull(scheduleLevel);
    }

    public static LeaderboardScope quarter(int year, int quarter) {
        return new LeaderboardScope(Type.QUARTER, year, quarter, null);
    }

    public static LeaderboardScope fitness(int year, int quarter, ScheduleLevel level) {
        return new LeaderboardScope(Type.FITNESS, year, quarter, level);
    }

    public String registryValue() {
        return type == Type.QUARTER ? "quarter|%d|%d".formatted(year, quarter)
                : "fitness|%d|%d|%s".formatted(year, quarter, scheduleLevel);
    }
}
