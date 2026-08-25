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
    public RolePermissionDTO.Response replace(
            String roleCode,
            Set<String> requestedCodes
    ) {
        Role role = roleRepository.findByIdForUpdate(roleCode)
                .orElseThrow(() ->
                        new IllegalArgumentException("Role not found")
                );

        SortedSet<String> desired =
                normalizeAndValidate(requestedCodes);

        SortedSet<String> current =
                rolePermissionRepository.findPermissionCodes(roleCode);

        Set<String> toAdd = new HashSet<>(desired);
        toAdd.removeAll(current);

        Set<String> toRemove = new HashSet<>(current);
        toRemove.removeAll(desired);

        if (!toAdd.isEmpty() || !toRemove.isEmpty()) {

            // 1. Revoke những quyền thừa
            if (!toRemove.isEmpty()) {
                rolePermissionRepository
                        .deleteByRoleCodeAndPermissionCodeIn(
                                roleCode,
                                toRemove
                        );
            }

            // 2. Grant những quyền thiếu
            if (!toAdd.isEmpty()) {
                List<Permission> permissions =
                        permissionRepository.findAllByCodeIn(toAdd);

                if (permissions.size() != toAdd.size()) {
                    throw new IllegalArgumentException(
                            "One or more permissions do not exist"
                    );
                }

                List<RolePermission> rolePermissions =
                        permissions.stream()
                                .map(permission ->
                                        new RolePermission(
                                                new RolePermission.Key(
                                                        roleCode,
                                                        permission.getPermissionId()
                                                ),
                                                role,
                                                permission
                                        )
                                )
                                .toList();

                rolePermissionRepository.saveAll(rolePermissions);
            }

            // 3. Tăng version đúng 1 lần cho cả lần thay đổi
            roleRepository.incrementPermissionVersion(roleCode);

            // Đồng bộ object đang giữ trong persistence context
            role.setPermissionVersion(
                    role.getPermissionVersion() + 1
            );
        }

        return new RolePermissionDTO.Response(
                roleCode,
                role.getPermissionVersion(),
                desired
        );
    }

    private SortedSet<String> normalizeAndValidate(Set<String> requestedCodes) {
        if (requestedCodes == null) {
            throw new IllegalArgumentException("Permission codes must not be null");
        }

        SortedSet<String> normalized = requestedCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(code -> code.toUpperCase(Locale.ROOT))
                .filter(code -> !code.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> definedCodes = Arrays.stream(PermissionDefinition.values())
                .map(PermissionDefinition::getCode)
                .collect(Collectors.toSet());

        if (!definedCodes.containsAll(normalized)) {
            throw new IllegalArgumentException(
                    "One or more permission codes are not defined by the backend"
            );
        }

        return normalized;
    }
}
