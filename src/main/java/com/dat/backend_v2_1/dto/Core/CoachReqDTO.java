package com.dat.backend_v2_1.dto.Core;

import com.dat.backend_v2_1.enums.Core.Belt;
import com.dat.backend_v2_1.enums.Core.CoachStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CoachReqDTO {
    @Data
    public static class CoachCreate{

        private CoachStatus coachStatus; // Optional, sẽ mặc định là ACTIVE nếu không được cung cấp

        @NotBlank(message = "Họ tên không được để trống")
        @Pattern(regexp = "^[a-zA-ZÀ-ỹÁ-Ỹ\\s]+$", message = "Họ tên chỉ được chứa chữ cái và khoảng trắng")
        @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
        private String fullName;

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
                message = "Số điện thoại không đúng định dạng Việt Nam")
        private String phoneNumber;

        @NotNull(message = "Ngày sinh không được để trống")
        @Past(message = "Ngày sinh phải là ngày trong quá khứ")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate birthDate;

        @NotNull(message = "Đai (Trình độ) không được để trống")
        private Belt belt;
    }

    @Data
    public static class CoachUpdate{
        @NotNull(message = "ID học viên không được để trống")
        private UUID userId;

        @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
                message = "Số điện thoại không đúng định dạng Việt Nam")
        private String phoneNumber;

        @Past(message = "Ngày sinh phải là ngày trong quá khứ")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate birthDate;

        private Belt belt;

        @Size(max = 50, message = "CCCD/CMND tối đa 50 ký tự")
        private String nationalCode;

        @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
        private String fullName;

        private CoachStatus coachStatus;
    }
}
