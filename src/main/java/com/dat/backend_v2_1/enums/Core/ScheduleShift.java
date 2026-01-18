package com.dat.backend_v2_1.enums.Core;

public enum ScheduleShift {
    CA_1("Ca 1"),
    CA_2("Ca 2");

    private final String displayName;

    ScheduleShift(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
