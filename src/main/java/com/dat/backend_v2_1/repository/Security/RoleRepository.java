package com.dat.backend_v2_1.repository.Security;

import com.dat.backend_v2_1.domain.Security.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByCode(@NotBlank(message = "Mã quyền không được để trống") @Size(max = 50, message = "Mã quyền không quá 50 ký tự") @Pattern(regexp = "^ROLE_[A-Z0-9_]+$", message = "Mã quyền phải bắt đầu bằng 'ROLE_' và viết hoa (VD: ROLE_ADMIN)") String code);
}
