package com.dat.ai_receptionist_web.dto.Skill;

import com.dat.ai_receptionist_web.dto.Core.CoachResDTO;
import com.dat.ai_receptionist_web.dto.Core.StudentResDTO;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FitnessRecordDTO {

    // 1. Đổi "Response" thành "Response" hoặc "Detail"
    // Khi gọi ở nơi khác sẽ là: FitnessRecordDTO.Response
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonIgnoreProperties(ignoreUnknown = true) // <--- THÊM DÒNG NÀY
    public static class Response {
        @NotNull(message = "ID không được để trống")
        Long id; // Đổi fitnessId thành id cho đúng chuẩn chung của REST API

        StudentResDTO.StudentSummary studentSummary; // Bỏ chữ Summary ở tên biến cho JSON trả về gọn hơn

        @NotNull
        Metrics metrics; // 2. Đổi "simpleResponse" thành "metrics" (Chỉ số) hoặc "result" (Kết quả)

        CoachResDTO.CoachSummary recordedByCoach;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ListResponse {
        Long id;
        StudentResDTO.StudentSummary studentSummary;
        ListMetrics metrics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ListMetrics {
        LocalDate assessmentDate;
        Integer duration;
        Integer amount;
        SkillLevel skillLevel;
        Integer durationLevel;
        Integer amountLevel;
        Integer fitnessLevel;
        Boolean isQualified;
    }

    // 3. Đổi "SimpleResponse" thành "Metrics" (Các chỉ số đo lường)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonIgnoreProperties(ignoreUnknown = true) // <--- THÊM DÒNG NÀY
    public static class Metrics {
        @NotNull(message = "Ngày tạo không được để trống")
        LocalDateTime createdAt;

        @NotNull(message = "Ngày đánh giá không được để trống")
        LocalDate assessmentDate;

        @Positive(message = "Thời gian (duration) phải lớn hơn 0")
        Integer duration;

        @Positive(message = "Số lượng (amount) phải lớn hơn 0")
        Integer amount;

        @NotNull(message = "Cấp độ kỹ năng không được để trống")
        SkillLevel skillLevel;

        @NotNull(message = "Thời gian cấp độ không được để trống")
        Integer durationLevel;

        @NotNull(message = "Số lượng cấp độ không được để trống")
        Integer amountLevel;

        @NotNull(message = "Cấp độ Thể lực & Tốc độ không được để trống")
        Integer fitnessLevel;

        @Builder.Default
        Boolean isQualified = false; // Dùng camelCase chuẩn Java
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CreateRequest {
        @NotNull(message = "Ngày đánh giá không được để trống")
        LocalDate assessmentDate;

        @NotNull(message = "ID học viên không được để trống")
        String studentCode;

        @NotNull(message = "Thời gian (duration) không được để trống")
        @Positive(message = "Thời gian phải lớn hơn 0")
        Integer duration;

        @NotNull(message = "Số lượng (amount) không được để trống")
        @Positive(message = "Số lượng phải lớn hơn 0")
        Integer amount;

        @NotNull(message = "Cấp độ kỹ năng không được để trống")
        SkillLevel skillLevel;

        @NotNull(message = "ID huấn luyện viên không được để trống")
        String staffCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UpdateRequest {
        @NotNull(message = "Ngày đánh giá không được để trống")
        LocalDate assessmentDate;

        @NotNull(message = "Thời gian (duration) không được để trống")
        @Positive(message = "Thời gian phải lớn hơn 0")
        Integer duration;

        @NotNull(message = "Số lượng (amount) không được để trống")
        @Positive(message = "Số lượng phải lớn hơn 0")
        Integer amount;

        @NotNull(message = "Cấp độ kỹ năng không được để trống")
        SkillLevel skillLevel;
    }
}
