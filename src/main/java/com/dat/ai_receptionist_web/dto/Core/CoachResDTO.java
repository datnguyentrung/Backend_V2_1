package com.dat.ai_receptionist_web.dto.Core;

import com.dat.ai_receptionist_web.dto.Operation.CoachAssignmentResDTO;
import com.dat.ai_receptionist_web.dto.Security.UserRes;
import com.dat.ai_receptionist_web.enums.Core.CoachStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

@Data
public class CoachResDTO {

    /**
     * DTO trả về thông tin chi tiết Coach
     * Bao gồm thông tin từ Coach và User (parent class)
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CoachDetail extends PersonDTO.PersonResponse {
        List<UserRes.UserDetail> userDetails; // Thông tin User liên quan đến Coach (nếu có)

        String email;

        // === Thông tin từ Coach (Child Entity) ===
        String staffCode;

        CoachStatus coachStatus; // Trạng thái công việc (ACTIVE, ON_LEAVE, etc.)

        List<CoachAssignmentResDTO.SimpleResponse> currentAssignments; // Thông tin phân công hiện tại (nếu có)
    }

    /**
     * DTO trả về thông tin tóm tắt Coach cho danh sách
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CoachSummary {
        UUID personId;
        String fullName;
        String staffCode;
        String email;
    }
}
