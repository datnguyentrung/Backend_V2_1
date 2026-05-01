package com.dat.backend_v2_1.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScoreRecalculateEvent {
    private final String studentCode;
    private final int quarter;
    private final int year;
}