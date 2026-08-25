package com.dat.ai_receptionist_web.dto.Core;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.enums.Core.PersonStatus;
import java.time.LocalDate;
import java.util.UUID;

public final class PersonDTO {
    private PersonDTO() {
    }

    public record CreateRequest(@NotNull String fullName, @NotNull Boolean gender, @NotNull LocalDate birthDate, @NotNull String email, @NotNull String nationalCode, @NotNull String faceImagePath, @NotNull String personCode, @NotNull Belt belt, @NotNull PersonStatus status, @NotNull LocalDate startDate) {
        public CreateRequest(String fullName, Boolean gender, LocalDate birthDate, String email,
                             String nationalCode, String personCode, Belt belt,
                             PersonStatus status, LocalDate startDate) {
            this(fullName, gender, birthDate, email, nationalCode, null, personCode, belt, status, startDate);
        }
    }

    public record UpdateRequest(@NotNull String fullName, @NotNull Boolean gender, @NotNull LocalDate birthDate, @NotNull String email, @NotNull String nationalCode, @NotNull String faceImagePath, @NotNull String personCode, @NotNull Belt belt, @NotNull PersonStatus status, @NotNull LocalDate startDate) {
    }

    public record Response(UUID personId, String fullName, Boolean gender, LocalDate birthDate, String email, String nationalCode, String personCode, Belt belt, PersonStatus status, LocalDate startDate, String faceImagePath, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
