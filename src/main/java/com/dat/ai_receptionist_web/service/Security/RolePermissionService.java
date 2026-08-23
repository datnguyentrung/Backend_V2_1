package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.*;
import com.dat.ai_receptionist_web.dto.Security.RolePermissionDTO;
import com.dat.ai_receptionist_web.repository.Security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;

@Service
@RequiredArgsConstructor
public class RolePermissionService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Transactional
    public RolePermissionDTO.Response replace(String roleCode, Set<String> requestedCodes) {
        Role role = roleRepository.findByIdForUpdate(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        SortedSet<String> desired = requestedCodes.stream()
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> defined = Arrays.stream(PermissionDefinition.values())
                .map(PermissionDefinition::getCode).collect(Collectors.toSet());
        if (!defined.containsAll(desired)) {
            throw new IllegalArgumentException("One or more permission codes are not defined by the backend");
        }
        SortedSet<String> current = rolePermissionRepository.findPermissionCodes(roleCode);
        if (!current.equals(desired)) {
            List<Permission> permissions = permissionRepository.findAllByCodeIn(desired);
            if (permissions.size() != desired.size()) {
                throw new IllegalArgumentException("One or more permissions do not exist");
            }
            rolePermissionRepository.deleteAll(rolePermissionRepository.findAllById_RoleId(roleCode));
            rolePermissionRepository.flush();
            permissions.forEach(permission -> rolePermissionRepository.save(new RolePermission(
                    new RolePermission.Key(roleCode, permission.getPermissionId()), role, permission)));
            roleRepository.incrementPermissionVersion(roleCode);
            role.setPermissionVersion(role.getPermissionVersion() + 1);
        }
        return new RolePermissionDTO.Response(roleCode, role.getPermissionVersion(), desired);
    }
}
