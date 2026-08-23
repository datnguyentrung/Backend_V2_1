package com.dat.ai_receptionist_web.repository.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Role r where r.code = :roleCode")
    Optional<Role> findByIdForUpdate(@Param("roleCode") String roleCode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Role r set r.permissionVersion = r.permissionVersion + 1 where r.code = :roleCode")
    int incrementPermissionVersion(@Param("roleCode") String roleCode);
}
