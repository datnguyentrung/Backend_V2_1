package com.dat.backend_v2_1.dto.Core;

import com.dat.backend_v2_1.dto.Operation.CoachAssignmentResDTO;
import com.dat.backend_v2_1.dto.Security.UserRes;
import com.dat.backend_v2_1.enums.Core.CoachStatus;
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
    public static class CoachDetail extends UserRes.UserDetail {
        UUID personId;

        String email;

        @Override
        @JsonIgnore
        public UUID getUserId() {
            return super.getUserId();
        }

        @Override
        @JsonIgnore
        public void setUserId(UUID userId) {
            super.setUserId(userId);
        }

        // === Thông tin từ Coach (Child Entity) ===
        String staffCode;

        String nationalCode;

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
