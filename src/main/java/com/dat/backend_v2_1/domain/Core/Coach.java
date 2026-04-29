package com.dat.backend_v2_1.domain.Core;

import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.enums.Core.Belt;
import com.dat.backend_v2_1.enums.Core.CoachStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder // Thay cho @Builder: Cho phép Coach.builder().email("...").build()
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coach", schema = "core")
// Chỉ định cột khóa chính của bảng Coach dùng để join với bảng User
@PrimaryKeyJoinColumn(name = "user_id")
@EqualsAndHashCode(callSuper = true) // So sánh object dựa trên cả field của User
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Coach extends User {

    @NotBlank(message = "Mã nhân viên không được để trống")
    @Size(max = 20)
    @Column(name = "staff_code", unique = true, nullable = false, length = 20)
    String staffCode;

    @Email(message = "Email không đúng định dạng") // Đây là Annotation kiểm tra hợp lệ
    @Size(max = 50, message = "Email không được vượt quá 50 ký tự")
    @Column(name = "email", length = 50)
    String email; // Kiểu dữ liệu phải là String

    @NotNull(message = "Đai không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "belt", length = 20)
    @Builder.Default
    Belt belt = Belt.C10;

    @Transient
    float[] faceEmbedding;

    // Lưu ý: User đã có status (UserStatus), Coach cũng có status (CoachStatus).
    // Nên đặt tên cột rõ ràng để tránh nhầm lẫn logic sau này.
    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "coach_status", nullable = false, length = 20)
    CoachStatus coachStatus = CoachStatus.ACTIVE; // Đổi tên biến để tránh Shadowing biến status của cha

    @Size(max = 50, message = "Mã hội viên tối đa 50 ký tự")
    @Column(name = "national_code", unique = true, length = 50)
    String nationalCode;
}