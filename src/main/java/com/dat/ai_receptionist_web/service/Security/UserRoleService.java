package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Security.UserRoleDTO;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRoleRepository;
import com.dat.ai_receptionist_web.util.error.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final UserRoleRepository userRoleRepository;

    @Transactional(rollbackFor = Exception.class)
    public UserRoleDTO.Response assignRole(UserRoleDTO.AssignRequest request) {
        return assignRole(request.getUserId(), request.getRoleCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public UserRoleDTO.Response assignRole(UUID userId, String roleCode) {
        User user = userRepository.findWithRolesByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with idUser: " + userId));
        String normalizedRoleCode = normalizeRoleCode(roleCode);
        if (userRoleRepository.countByUserIdAndRoleCode(userId, normalizedRoleCode) == 0) {
            user.getRoles().add(roleService.getRoleReferenceByCode(normalizedRoleCode));
            user = userRepository.save(user);
        }
        return toResponse(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserRoleDTO.Response assignRoles(User user, Set<String> roleCodes) {
        roleCodes.forEach(roleCode -> user.getRoles().add(roleService.getRoleReferenceByCode(normalizeRoleCode(roleCode))));
        return toResponse(userRepository.save(user));
    }

    private String normalizeRoleCode(String roleCode) {
        return roleCode == null || roleCode.startsWith("ROLE_") ? roleCode : "ROLE_" + roleCode;
    }

    private UserRoleDTO.Response toResponse(User user) {
        return UserRoleDTO.Response.builder()
                .userId(user.getUserId())
                .roleCodes(user.getRoles().stream()
                        .map(Role::getCode)
                        .collect(Collectors.toCollection(java.util.TreeSet::new)))
                .build();
    }
}
