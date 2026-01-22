package com.dat.backend_v2_1.domain.Core;

import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder // Bắt buộc dùng SuperBuilder vì kế thừa từ User
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student", schema = "core")
@PrimaryKeyJoinColumn(name = "user_id") // Khóa chính cũng là FK trỏ về bảng User
@EqualsAndHashCode(callSuper = true) // So sánh object bao gồm cả các field của User
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Student extends User {

    @NotBlank(message = "Mã học viên không được để trống")
    @Size(max = 50, message = "Mã học viên tối đa 20 ký tự")
    @Column(name = "student_code", nullable = false, unique = true, length = 50)
    String studentCode;

//    @NotBlank(message = "CCCD/CMND không được để trống")
    @Size(max = 50, message = "CCCD/CMND tối đa 12 ký tự")
    @Column(name = "national_code", nullable = true, unique = true, length = 50)
    String nationalCode;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    @Column(name = "full_name", nullable = false, length = 100)
    String fullName;

    @NotNull(message = "Ngày bắt đầu tập không được để trống")
    @PastOrPresent(message = "Ngày bắt đầu không được ở tương lai")
    @Column(name = "start_date", nullable = false)
    LocalDate startDate = LocalDate.now();

    // Đổi tên biến status -> studentStatus để tránh trùng với User.status
    // User.status: Active/Locked (Trạng thái tài khoản hệ thống)
    // Student.studentStatus: Studying/Paused/Dropout (Trạng thái học tập)
    @NotNull(message = "Trạng thái học viên không được để trống")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "student_status", nullable = false, length = 20)
    StudentStatus studentStatus = StudentStatus.ACTIVE;

    @NotNull(message = "Chi nhánh không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false) // FK trỏ sang bảng Branch
    @ToString.Exclude
    Branch branch;
}
