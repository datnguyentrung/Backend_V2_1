package com.dat.ai_receptionist_web.dto.Core;

import com.dat.ai_receptionist_web.enums.Core.Belt;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.UUID;

public class PersonDTO {

    private PersonDTO() {
    }

    /**
     * Common Person data used internally while creating a concrete Person subtype.
     */
    public record PersonCreationData(
            String fullName,
            LocalDate birthDate,
            Belt belt,
            String nationalCode,
            String email
    ) {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SearchItem {
        UUID personId;
        String fullName;
        LocalDate birthDate;
        String belt;
        String personType;
        String code;
        String status;
    }

    public interface FaceCheckInResponse {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FaceEmbeddingUpdateResponse {
        UUID personId;
        Integer dimension;
        String model;
        String faceImagePath;
        LocalDateTime updatedAt;
    }

    public record FaceImageUrlResponse(String faceImageUrl, Duration expiresIn) {
    }

}
