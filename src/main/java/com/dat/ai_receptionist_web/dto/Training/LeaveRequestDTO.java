package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.enums.Training.LeaveRequestStatus;
import com.dat.ai_receptionist_web.enums.Training.RequesterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class LeaveRequestDTO {
    private LeaveRequestDTO() {
    }

    public record CreateRequest(
            @NotNull UUID personId,
            @NotNull RequesterType requesterType,
            LocalDate leaveDate,
            UUID leaveClassSessionId,
            UUID makeupClassSessionId,
            @NotBlank @Size(max = 1000) String leaveContext
    ) {
    }

    public record ReviewCommand(
            @Size(max = 1000) String reviewNote
    ) {
    }

    public record Response(
            UUID leaveRequestId,
            UUID personId,
            RequesterType requesterType,
            LocalDate leaveDate,
            UUID leaveClassSessionId,
            UUID makeupClassSessionId,
            String leaveContext,
            LeaveRequestStatus status,
            UUID createdByUserId,
            UUID reviewedByUserId,
            LocalDateTime reviewedAt,
            String reviewNote,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
