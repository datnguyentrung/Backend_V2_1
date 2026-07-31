package com.dat.ai_receptionist_web.dto.Security;

import com.dat.ai_receptionist_web.dto.Operation.CoachAssignmentResDTO;
import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRes {
    UserInfo userInfo;
    UserProfile userProfile;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserInfo {
        UUID idUser;
        String userCode;
        String idRole;
        List<CoachAssignmentResDTO.Response> assignedClasses; // Dành riêng cho HLV, có thể là null hoặc rỗng nếu không phải HLV
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserProfile {
        LocalDate birthDate;
        Boolean isActive;
        String name;
        String phone;
        Belt belt;
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserDetail {
        UUID userId;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate birthDate;

        String phoneNumber;

        Belt belt;

        UserStatus status;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        LocalDateTime createdAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        LocalDateTime updatedAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        LocalDateTime lastLoginAt;

        List<String> roles;

        String fullName;

        RelationshipType relationshipType;

        Boolean active;

        @Builder.Default
        Boolean gender = Boolean.TRUE; // true = nam, false = nữ
    }
}
