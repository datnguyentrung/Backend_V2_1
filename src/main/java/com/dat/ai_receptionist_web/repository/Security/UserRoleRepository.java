package com.dat.ai_receptionist_web.repository.Security;

import com.dat.ai_receptionist_web.domain.Security.UserRole;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.Key> {
    List<UserRole> findAllById_UserId(UUID userId);

    @Query("select ur.role.code from UserRole ur where ur.id.userId = :userId order by ur.role.code")
    SortedSet<String> findRoleCodes(@Param("userId") UUID userId);

    void deleteById_UserIdAndRole_CodeIn(
            UUID userId,
            Set<String> roleCodes
    );
}
