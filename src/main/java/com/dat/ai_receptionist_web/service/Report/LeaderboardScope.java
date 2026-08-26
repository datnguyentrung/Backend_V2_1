package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import java.util.Objects;

public record LeaderboardScope(Type type, int year, int quarter, ScheduleLevel scheduleLevel) {
    public enum Type { QUARTER, FITNESS }

    public LeaderboardScope {
        if (quarter < 1 || quarter > 4) throw new IllegalArgumentException("Quarter must be between 1 and 4");
        if (type == Type.FITNESS) Objects.requireNonNull(scheduleLevel);
    }

    /**
     * Tác dụng: Thực hiện logic quarter của lớp hiện tại.
     * Input: Nhận int year, int quarter từ caller hoặc request.
     * Output: Trả về LeaderboardScope theo kết quả xử lý.
     */
    public static LeaderboardScope quarter(int year, int quarter) {
        return new LeaderboardScope(Type.QUARTER, year, quarter, null);
    }

    /**
     * Tác dụng: Thực hiện logic fitness của lớp hiện tại.
     * Input: Nhận int year, int quarter, ScheduleLevel level từ caller hoặc request.
     * Output: Trả về LeaderboardScope theo kết quả xử lý.
     */
    public static LeaderboardScope fitness(int year, int quarter, ScheduleLevel level) {
        return new LeaderboardScope(Type.FITNESS, year, quarter, level);
    }

    /**
     * Tác dụng: Thực hiện logic registryValue của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Trả về String theo kết quả xử lý.
     */
    public String registryValue() {
        return type == Type.QUARTER ? "quarter|%d|%d".formatted(year, quarter)
                : "fitness|%d|%d|%s".formatted(year, quarter, scheduleLevel);
    }
}


