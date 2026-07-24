package com.dat.ai_receptionist_web.dto.Core;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
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
}
