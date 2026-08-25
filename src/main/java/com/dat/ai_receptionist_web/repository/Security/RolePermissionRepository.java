package com.dat.ai_receptionist_web.repository.Security;

import com.dat.ai_receptionist_web.domain.Security.RolePermission;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.Key> {
    List<RolePermission> findAllById_RoleId(String roleId);

    @Query("select rp.permission.code from RolePermission rp where rp.id.roleId = :role order by rp.permission.code")
    SortedSet<String> findPermissionCodes(@Param("role") String roleCode);

    void deleteByRoleCodeAndPermissionCodeIn(String roleCode, Set<String> toRemove);
}
