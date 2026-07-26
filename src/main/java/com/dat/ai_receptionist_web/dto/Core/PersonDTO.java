package com.dat.ai_receptionist_web.dto.Core;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class PersonDTO {

    private PersonDTO() {
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
        LocalDateTime updatedAt;
    }

}
