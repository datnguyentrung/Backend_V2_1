package com.dat.backend_v2_1.domain.Security;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "role", schema = "security")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Role {

    @Id
    @NotBlank(message = "Mã quyền không được để trống")
    @Size(max = 50, message = "Mã quyền không quá 50 ký tự")
    // Convention của Spring Security thường bắt đầu bằng ROLE_ (VD: ROLE_ADMIN)
    @Pattern(regexp = "^ROLE_[A-Z0-9_]+$", message = "Mã quyền phải bắt đầu bằng 'ROLE_' và viết hoa (VD: ROLE_ADMIN)")
    @Column(name = "role_code", nullable = false, length = 50)
    String code; // Tên biến trong Java là 'code', DB là 'role_code' - Đây là khóa chính

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 100)
    @Column(name = "role_name", unique = true, nullable = false, length = 100)
    String name; // Tên hiển thị (VD: Quản trị viên, Học viên) - Java dùng 'name' cho chuẩn

    @Size(max = 255)
    @Column(name = "description")
    String description;

    // --- Override equals/hashCode ---
    // Giúp so sánh 2 Role dựa trên code (nghiệp vụ) thay vì vị trí bộ nhớ
    // Rất hữu ích khi dùng trong Set<Role> hoặc SecurityContext

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(code, role.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}