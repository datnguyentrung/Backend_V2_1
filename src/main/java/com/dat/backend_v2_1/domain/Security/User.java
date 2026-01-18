package com.dat.backend_v2_1.domain.Security;

import com.dat.backend_v2_1.enums.Core.Belt;
import com.dat.backend_v2_1.enums.Security.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder // Thay cho @Builder để hỗ trợ kế thừa
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
// Quan trọng: Định nghĩa chiến lược kế thừa.
// JOINED: Tạo 2 bảng riêng biệt, bảng con trỏ ID về bảng cha.
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "user", schema = "security") // Tên bảng nên viết thường (lowercase)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(generator = "uuid-hibernate-generator")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    UUID userId;

    @NotNull
    @Column(name = "password_hash", nullable = false)
    String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default // Giá trị mặc định khi dùng Builder
    @Column(name = "status", nullable = false, length = 20)
    UserStatus status = UserStatus.ACTIVE;

    @CreatedDate // Tự động set khi tạo mới
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "last_login_at")
    Instant lastLoginAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    @ToString.Exclude
    Role role;

    @NotBlank
    @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
            message = "Số điện thoại không đúng định dạng VN")
    @Column(name = "phone_number", length = 10)
    String phoneNumber;

    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh không hợp lệ (Phải là ngày trong quá khứ)")
    @Column(name = "birth_date", nullable = false)
    LocalDate birthDate;

    @NotNull(message = "Đai không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "belt", length = 20)
    Belt belt;
}