package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.*;
import com.dat.ai_receptionist_web.dto.Security.UserRoleDTO;
import com.dat.ai_receptionist_web.repository.Security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserRoleService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    public UserRoleDTO.Response assignRole(UserRoleDTO.AssignRequest request) {
        User user = userRepository.findByIdForUpdate(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String roleCode = request.getRoleCode().trim().toUpperCase(Locale.ROOT);
        Role role = roleRepository.findById(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleCode));
        UserRole.Key key = new UserRole.Key(user.getUserId(), roleCode);
        if (!userRoleRepository.existsById(key)) {
            userRoleRepository.save(new UserRole(key, user, role));
            userRepository.incrementAuthorizationVersion(user.getUserId());
        }
        return new UserRoleDTO.Response(user.getUserId(), userRoleRepository.findRoleCodes(user.getUserId()));
    }

    @Transactional
    public UserRoleDTO.Response replaceRoles(UUID userId, Set<String> requestedCodes) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        SortedSet<String> desired = new TreeSet<>();
        requestedCodes.forEach(code -> desired.add(code.trim().toUpperCase(Locale.ROOT)));
        SortedSet<String> current = userRoleRepository.findRoleCodes(userId);
        if (current.equals(desired)) return new UserRoleDTO.Response(userId, current);

        List<Role> roles = roleRepository.findAllById(desired);
        if (roles.size() != desired.size()) throw new IllegalArgumentException("One or more roles do not exist");
        userRoleRepository.deleteAll(userRoleRepository.findAllById_UserId(userId));
        userRoleRepository.flush();
        roles.forEach(role -> userRoleRepository.save(
                new UserRole(new UserRole.Key(userId, role.getCode()), user, role)));
        userRepository.incrementAuthorizationVersion(userId);
        return new UserRoleDTO.Response(userId, desired);
    }
}
