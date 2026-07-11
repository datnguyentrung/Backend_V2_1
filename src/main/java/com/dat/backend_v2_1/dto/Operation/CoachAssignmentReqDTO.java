package com.dat.backend_v2_1.dto.Operation;

import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CoachAssignmentReqDTO {

    @Data
    public static class CreateRequest {
        @NotNull(message = "Huấn luyện viên không được để trống")
        private String coachId;

        @NotEmpty(message = "Vui lòng chọn ít nhất một lớp học")
        private List<String> scheduleIds;

        @NotNull(message = "Ngày phân công không được để trống")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate assignmentDate;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        private String note;

        @AssertTrue(message = "Ngày bắt đầu không được lớn hơn ngày kết thúc")
        public boolean isDateRangeValid() {
            return assignmentDate == null || endDate == null || !assignmentDate.isAfter(endDate);
        }
    }

    @Data
    public static class UpdateRequest {
        @NotNull(message = "Trạng thái không được để trống")
        private CoachAssignmentStatus status;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate assignmentDate;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        private String note;

        @AssertTrue(message = "Ngày bắt đầu không được lớn hơn ngày kết thúc")
        public boolean isDateRangeValid() {
            return assignmentDate == null || endDate == null || !assignmentDate.isAfter(endDate);
        }
    }

    @Data
    public static class FilterRequest {
        private String coachId;
        private String classScheduleId;
        private Integer branchId;
        private CoachAssignmentStatus status;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate startDate;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate endDate;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate effectiveDate;
        private String search;
    }
}
