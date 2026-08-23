package com.dat.ai_receptionist_web.dto.Core;

import com.dat.ai_receptionist_web.enums.Core.*;
import jakarta.validation.constraints.*;

import java.time.*;
import java.util.UUID;

public final class PersonDTO {
    private PersonDTO() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 100) String fullName,
            Boolean gender,
            LocalDate birthDate,
            @Email @Size(max = 100) String email,
            @Size(max = 50) String nationalCode,
            @NotBlank @Size(max = 50) String personCode,
            @NotNull Belt belt,
            @NotNull PersonStatus status,
            LocalDate startDate) {
    }

    public record Response(UUID personId, String fullName, Boolean gender, LocalDate birthDate,
                           String email, String nationalCode, String personCode, Belt belt,
                           PersonStatus status, LocalDate startDate, String faceImagePath,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
