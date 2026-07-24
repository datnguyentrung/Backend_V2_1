package com.dat.ai_receptionist_web.dto.Security;

import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

public class UserProfileDTO {

    private UserProfileDTO() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CreateRequest {
        @NotNull(message = "User id must not be null")
        UUID userId;

        @NotNull(message = "Person id must not be null")
        UUID personId;

        @Builder.Default
        RelationshipType relationshipType = RelationshipType.OWNER;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Response {
        UUID userProfileId;
        UUID userId;
        UUID personId;
        RelationshipType relationshipType;
        Boolean active;
    }
}
