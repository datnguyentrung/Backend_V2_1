package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import com.dat.ai_receptionist_web.repository.Security.AuthSessionRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorizationService {
    private static final Set<String> DEFINED_PERMISSIONS = Arrays.stream(PermissionDefinition.values())
            .map(PermissionDefinition::getCode)
            .collect(Collectors.toUnmodifiableSet());

    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;

    @Transactional(readOnly = true)
    public AuthorizationSnapshot loadSnapshot(UUID userId) {
        List<UserRepository.AuthorizationRow> rows = userRepository.findAuthorizationRows(userId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        UserRepository.AuthorizationRow head = rows.getFirst();
        SortedSet<String> roles = new TreeSet<>();
        SortedMap<String, Long> versions = new TreeMap<>();
        SortedSet<String> permissions = new TreeSet<>();
        for (UserRepository.AuthorizationRow row : rows) {
            if (row.getRoleCode() != null) {
                roles.add(row.getRoleCode());
                versions.put(row.getRoleCode(), row.getPermissionVersion());
            }
            if (row.getPermissionCode() != null && DEFINED_PERMISSIONS.contains(row.getPermissionCode())) {
                permissions.add(row.getPermissionCode());
            }
        }
        return new AuthorizationSnapshot(
                head.getUserId(),
                head.getPhoneNumber(),
                UserStatus.valueOf(head.getUserStatus()),
                head.getAuthorizationVersion(),
                Collections.unmodifiableSortedSet(roles),
                Collections.unmodifiableSortedMap(versions),
                Collections.unmodifiableSortedSet(permissions)
        );
    }

    @Transactional(readOnly = true)
    public void validateAccessToken(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID sessionId = UUID.fromString(jwt.getClaimAsString("authSessionId"));
        List<AuthSessionRepository.AccessStateRow> rows = authSessionRepository.findAccessState(sessionId, userId);
        if (rows.isEmpty()) {
            throw new StaleAccessTokenException("Session does not exist");
        }

        AuthSessionRepository.AccessStateRow head = rows.getFirst();
        if (head.getRevoked() || !head.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new StaleAccessTokenException("Session is revoked or expired");
        }
        if (!UserStatus.ACTIVE.name().equals(head.getUserStatus())) {
            throw new StaleAccessTokenException("User is not active");
        }
        if (numberClaim(jwt, "authorizationVersion") != head.getAuthorizationVersion()) {
            throw new StaleAccessTokenException("User authorization version changed");
        }

        Set<String> currentRoles = new TreeSet<>();
        Map<String, Long> currentVersions = new TreeMap<>();
        for (AuthSessionRepository.AccessStateRow row : rows) {
            if (row.getRoleCode() != null) {
                currentRoles.add(row.getRoleCode());
                currentVersions.put(row.getRoleCode(), row.getPermissionVersion());
            }
        }
        Set<String> tokenRoles = new TreeSet<>(Optional.ofNullable(jwt.getClaimAsStringList("roles")).orElse(List.of()));
        if (!currentRoles.equals(tokenRoles) || !currentVersions.equals(versionClaim(jwt))) {
            throw new StaleAccessTokenException("Role permission version changed");
        }

        UUID tokenContext = optionalUuid(jwt.getClaimAsString("activeUserPersonId"));
        if (!Objects.equals(tokenContext, head.getActiveUserPersonId())) {
            throw new StaleAccessTokenException("Active person context changed");
        }
        if (head.getActiveUserPersonId() != null && !head.getActiveUserPersonActive()) {
            throw new StaleAccessTokenException("Active person context is disabled");
        }
    }

    private long numberClaim(Jwt jwt, String name) {
        Number value = jwt.getClaim(name);
        if (value == null) {
            throw new StaleAccessTokenException("Missing " + name);
        }
        return value.longValue();
    }

    private Map<String, Long> versionClaim(Jwt jwt) {
        Map<String, Object> raw = jwt.getClaim("rolePermissionVersions");
        if (raw == null) {
            throw new StaleAccessTokenException("Missing role permission versions");
        }
        Map<String, Long> result = new TreeMap<>();
        raw.forEach((key, value) -> result.put(key, ((Number) value).longValue()));
        return result;
    }

    private UUID optionalUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    public static class StaleAccessTokenException extends RuntimeException {
        public StaleAccessTokenException(String message) {
            super(message);
        }
    }
}
