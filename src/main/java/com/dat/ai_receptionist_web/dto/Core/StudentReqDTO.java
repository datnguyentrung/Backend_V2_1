package com.dat.ai_receptionist_web.dto.Core;

import com.dat.ai_receptionist_web.dto.Operation.StudentEnrollmentReqDTO;
import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.enums.Core.StudentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class StudentReqDTO {
    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class StudentCreate {
        MultipartFile file;

        // 1. Dùng cho các field
        // Có thể chưa được liên đoàn cấp mã số, nên không bắt buộc phải có
        String nationalCode;

        @NotNull(message = "Trạng thái học viên không được để trống")
        StudentStatus studentStatus;

        @NotBlank(message = "Họ tên không được để trống")
        @Pattern(regexp = "^[a-zA-ZÀ-ỹÁ-Ỹ\\s]+$", message = "Họ tên chỉ được chứa chữ cái và khoảng trắng")
        @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
        String fullName;

        // 2. Định dạng ngày tháng rõ ràng cho Frontend (ISO-8601 hoặc custom)
        @NotNull(message = "Ngày nhập học không được để trống")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate startDate;

        // 3. Sửa lỗi: Long dùng @NotNull, thêm @Positive để tránh số âm
        @NotNull(message = "Cơ sở (Branch) không được để trống")
        @Positive(message = "ID cơ sở không hợp lệ")
        Long branchId;

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
                message = "Số điện thoại không đúng định dạng Việt Nam")
        String phoneNumber;

        @NotNull(message = "Ngày sinh không được để trống")
        @Past(message = "Ngày sinh phải là ngày trong quá khứ")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate birthDate;

        @NotNull(message = "Đai (Trình độ) không được để trống")
        Belt belt;

        StudentEnrollmentReqDTO.CreateRequest enrollmentRequest;
    }

    /**
     * DTO cho việc cập nhật thông tin Student
     * Các field nullable sẽ không được cập nhật nếu không được gửi lên
     */
    @Data
    public static class StudentUpdate {
        MultipartFile file;

        UUID personId;

        // === Thông tin có thể cập nhật từ User ===
//        @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
//                message = "Số điện thoại không đúng định dạng Việt Nam")
//        String phoneNumber;

        @Past(message = "Ngày sinh phải là ngày trong quá khứ")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate birthDate;

        Belt belt;

        // === Thông tin có thể cập nhật từ Student ===
        @Size(max = 50, message = "CCCD/CMND tối đa 50 ký tự")
        String nationalCode;

        @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
        String fullName;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate startDate;

        StudentStatus studentStatus;

        @Positive(message = "ID cơ sở không hợp lệ")
        Long branchId;
    }

}
