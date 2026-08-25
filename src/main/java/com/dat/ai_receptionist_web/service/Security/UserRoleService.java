package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.*;
import com.dat.ai_receptionist_web.dto.Security.UserRoleDTO;
import com.dat.ai_receptionist_web.repository.Security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    public UserRoleDTO.Response replaceRoles(
            UUID userId,
            Set<String> requestedCodes
    ) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        SortedSet<String> desired = requestedCodes.stream()
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));

        SortedSet<String> current =
                userRoleRepository.findRoleCodes(userId);

        Set<String> toAdd = new HashSet<>(desired);
        toAdd.removeAll(current);

        Set<String> toRemove = new HashSet<>(current);
        toRemove.removeAll(desired);

        if (!toAdd.isEmpty() || !toRemove.isEmpty()) {

            if (!toRemove.isEmpty()) {
                userRoleRepository.deleteById_UserIdAndRole_CodeIn(
                        userId,
                        toRemove
                );
            }

            if (!toAdd.isEmpty()) {
                List<Role> roles = roleRepository.findAllById(toAdd);

                if (roles.size() != toAdd.size()) {
                    throw new IllegalArgumentException(
                            "One or more roles do not exist"
                    );
                }

                List<UserRole> userRoles = roles.stream()
                        .map(role ->
                                new UserRole(
                                        new UserRole.Key(
                                                userId,
                                                role.getCode()
                                        ),
                                        user,
                                        role
                                )
                        )
                        .toList();

                userRoleRepository.saveAll(userRoles);
            }

            userRepository.incrementAuthorizationVersion(userId);
        }

        return new UserRoleDTO.Response(userId, desired);
    }
}
