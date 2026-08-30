package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.enums.Training.BeltExamResult;
import com.dat.ai_receptionist_web.enums.Training.BeltExamType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class BeltExamDTO {
    private BeltExamDTO() {
    }

    public record CreateRequest(
            @NotNull UUID personId,
            @NotNull Belt fromBelt,
            @NotNull Belt targetBelt,
            @NotNull @Min(1) Integer year,
            @NotNull @Min(1) @Max(4) Integer quarter,
            LocalDate examDate,
            BeltExamResult result,
            @Size(max = 1000) String note,
            @NotNull UUID createdByUserId,
            @NotNull BeltExamType type
    ) {
    }

    public record UpdateRequest(
            @NotNull UUID personId,
            @NotNull Belt fromBelt,
            @NotNull Belt targetBelt,
            @NotNull @Min(1) Integer year,
            @NotNull @Min(1) @Max(4) Integer quarter,
            LocalDate examDate,
            @NotNull BeltExamResult result,
            @Size(max = 1000) String note,
            @NotNull UUID createdByUserId,
            @NotNull BeltExamType type
    ) {
    }

    public record Response(
            UUID beltExamId,
            UUID personId,
            Belt fromBelt,
            Belt targetBelt,
            Integer year,
            Integer quarter,
            LocalDate examDate,
            BeltExamResult result,
            String note,
            UUID createdByUserId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            BeltExamType type
    ) {
    }
}
