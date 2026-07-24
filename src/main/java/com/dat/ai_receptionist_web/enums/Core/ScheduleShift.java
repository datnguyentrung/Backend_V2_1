package com.dat.ai_receptionist_web.enums.Core;

import lombok.Getter;

@Getter
public enum ScheduleShift {
    CA_1("Ca 1"),
    CA_2("Ca 2");

    private final String displayName;

    ScheduleShift(String displayName) {
        this.displayName = displayName;
    }

}
