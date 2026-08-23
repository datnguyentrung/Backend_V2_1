package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.repository.Security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {
    @Mock UserRepository userRepository;
    @Mock AuthSessionRepository sessionRepository;
    @Mock AuthSessionRepository.AccessStateRow row;
    @InjectMocks AuthorizationService service;

    @Test
    void validatesSessionAndRoleVersionsWithOneDatabaseQuery() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(row.getRevoked()).thenReturn(false);
        when(row.getExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
        when(row.getUserStatus()).thenReturn("ACTIVE");
        when(row.getAuthorizationVersion()).thenReturn(3L);
        when(row.getRoleCode()).thenReturn("MANAGER");
        when(row.getPermissionVersion()).thenReturn(7L);
        when(row.getActiveUserPersonId()).thenReturn(null);
        when(sessionRepository.findAccessState(sessionId, userId)).thenReturn(List.of(row));

        service.validateAccessToken(jwt(userId, sessionId, 3L, List.of("MANAGER"), Map.of("MANAGER", 7L)));

        verify(sessionRepository, times(1)).findAccessState(sessionId, userId);
        verifyNoInteractions(userRepository);
    }

    @Test
    void rejectsStaleRolePermissionVersion() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(row.getRevoked()).thenReturn(false);
        when(row.getExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
        when(row.getUserStatus()).thenReturn("ACTIVE");
        when(row.getAuthorizationVersion()).thenReturn(3L);
        when(row.getRoleCode()).thenReturn("MANAGER");
        when(row.getPermissionVersion()).thenReturn(8L);
        when(sessionRepository.findAccessState(sessionId, userId)).thenReturn(List.of(row));

        assertThatThrownBy(() -> service.validateAccessToken(
                jwt(userId, sessionId, 3L, List.of("MANAGER"), Map.of("MANAGER", 7L))))
                .isInstanceOf(AuthorizationService.StaleAccessTokenException.class);
    }

    private Jwt jwt(UUID userId, UUID sessionId, long authVersion,
                    List<String> roles, Map<String, Long> versions) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token").header("alg", "none")
                .subject(userId.toString()).issuedAt(now).expiresAt(now.plusSeconds(60))
                .claim("authSessionId", sessionId.toString())
                .claim("authorizationVersion", authVersion)
                .claim("roles", roles)
                .claim("rolePermissionVersions", versions)
                .build();
    }
}
