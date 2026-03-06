package com.dat.backend_v2_1.domain.Core;

import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.enums.Core.CoachStatus;
import jakarta.persistence.*;
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

    // Lưu ý: User đã có status (UserStatus), Coach cũng có status (CoachStatus).
    // Nên đặt tên cột rõ ràng để tránh nhầm lẫn logic sau này.
    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "coach_status", nullable = false, length = 20)
    CoachStatus coachStatus = CoachStatus.ACTIVE; // Đổi tên biến để tránh Shadowing biến status của cha
}