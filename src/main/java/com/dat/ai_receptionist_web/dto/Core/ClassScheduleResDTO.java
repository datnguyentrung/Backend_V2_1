package com.dat.ai_receptionist_web.dto.Core;

import com.dat.ai_receptionist_web.enums.Core.*;
import com.dat.ai_receptionist_web.enums.Core.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
public class ClassScheduleResDTO {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ClassScheduleSummary {
        String scheduleId;
        String branchName;
        ScheduleLocation scheduleLocation;
        ScheduleLevel scheduleLevel;
        ScheduleShift scheduleShift;
        BigDecimal monthlyFee;
        BigDecimal quarterlyFee;

        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime;

        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime;

        Weekday weekday;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClassScheduleDetail {
        String scheduleId;

        Long branchId;
        String branchName;

        List<CoachResDTO.CoachSummary> coaches;

        ScheduleLevel scheduleLevel;
        ScheduleShift scheduleShift;
        ScheduleLocation scheduleLocation;
        ScheduleStatus scheduleStatus;
        BigDecimal monthlyFee;
        BigDecimal quarterlyFee;

        Weekday weekday;

        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime;

        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime;

        Integer totalStudents;
    }
}
