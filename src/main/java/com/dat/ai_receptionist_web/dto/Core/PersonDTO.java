package com.dat.ai_receptionist_web.dto.Core;

import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.dto.Core.CoachResDTO.CoachSummary;
import com.dat.ai_receptionist_web.dto.Core.StudentResDTO.StudentSummary;
import com.dat.ai_receptionist_web.dto.Operation.CoachTimesheetDTO;
import com.dat.ai_receptionist_web.dto.Operation.StudentAttendanceDTO;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class PersonDTO {

    private PersonDTO() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PersonResponse {
        UUID personId;
        String fullName;
        Boolean gender;
        LocalDate birthDate;
        String nationalCode;
        String email;
        Belt belt;
        String faceImagePath;
        String avatarUrl;
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

    /**
     * @deprecated Kept only for source compatibility with existing operation DTOs.
     * Face check-in now returns {@link FaceCheckInResult}.
     */
    @Deprecated
    public interface FaceCheckInResponse {
    }

    /**
     * Result of a face check-in attempt once a person has been identified.
     * A failed operational check-in is represented in this payload rather than
     * as an HTTP error, so the client can still show the identified person.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FaceCheckInResult {
        String personType;
        boolean checkInSuccess;
        String checkInErrorCode;
        String checkInErrorMessage;
        StudentResDTO.StudentDetail studentDetail;
        CoachResDTO.CoachDetail coachDetail;
        StudentAttendanceDTO.Response studentAttendance;
        CoachTimesheetDTO.Response coachTimesheet;
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
        String avatarUrl;
        LocalDateTime updatedAt;
    }

    public record FaceImageUrlResponse(String avatarUrl) {
    }

}
