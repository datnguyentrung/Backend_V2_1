package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Core.Coach;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Core.Student;
import com.dat.ai_receptionist_web.domain.Security.AuthToken;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Security.UserProfile;
import com.dat.ai_receptionist_web.dto.Security.LoginRes;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.repository.Security.AuthTokenRepository;
import com.dat.ai_receptionist_web.repository.Security.UserProfileRepository;
import com.dat.ai_receptionist_web.util.RefreshTokenUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private final AuthTokenRepository authTokenRepository;
    private final UserService userService;
    private final UserProfileRepository userProfileRepository;
    private final EntityManager entityManager;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    @Transactional
    @CacheEvict(value = "fcmTokensByRole", allEntries = true)
    public AuthToken createSession(User user, String refreshTokenHash, String deviceInfo, String fcmToken,
                                   LoginRes.UserContextRes activeContext) {
        AuthToken token = new AuthToken();
        token.setSessionId(UUID.randomUUID().toString());
        token.setUser(user);
        token.setRefreshTokenHash(refreshTokenHash);
        token.setDeviceInfo(deviceInfo);
        token.setFcmToken(fcmToken);
        token.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration));
        applyContext(token, activeContext);
        return authTokenRepository.save(token);
    }

    @Transactional
    public AuthToken rotateRefreshToken(String rawRefreshToken, String newRawRefreshToken) {
        String oldHash = RefreshTokenUtil.sha256(rawRefreshToken);
        AuthToken token = authTokenRepository.findByRefreshTokenHashForUpdate(oldHash)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        token.setRefreshTokenHash(RefreshTokenUtil.sha256(newRawRefreshToken));
        token.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration));
        token.setLastUsedAt(LocalDateTime.now());
        return token;
    }

    public AuthToken getByRefreshTokenHash(String rawRefreshToken) {
        return authTokenRepository.findByRefreshTokenHash(RefreshTokenUtil.sha256(rawRefreshToken))
                .orElse(null);
    }

    public AuthToken getBySessionId(String sessionId) {
        return authTokenRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    }

    @Transactional
    @CacheEvict(value = "fcmTokensByRole", allEntries = true)
    public void revokeByRawRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        AuthToken token = getByRefreshTokenHash(rawRefreshToken);
        if (token != null) {
            revoke(token);
        }
    }

    @Transactional
    @CacheEvict(value = "fcmTokensByRole", allEntries = true)
    public void revokeBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        authTokenRepository.findBySessionId(sessionId).ifPresent(this::revoke);
    }

    @Transactional
    @CacheEvict(value = "fcmTokensByRole", allEntries = true)
    public void revokeAllByUserId(UUID userId) {
        authTokenRepository.findAllByUser_UserId(userId).forEach(this::revoke);
    }

    @Transactional
    public void updateContext(String sessionId, LoginRes.UserContextRes context) {
        AuthToken token = getBySessionId(sessionId);
        applyContext(token, context);
    }

    @Transactional
    @CacheEvict(value = "fcmTokensByRole", allEntries = true)
    public void updateFcmTokenForSession(String sessionId, String fcmToken) {
        AuthToken token = getBySessionId(sessionId);
        token.setFcmToken(fcmToken);
    }

    public List<LoginRes.UserContextRes> getActiveContexts(UUID userId) {
        return userProfileRepository.findActiveContextRowsByUserId(userId).stream()
                .map(this::toContext)
                .toList();
    }

    public LoginRes.UserContextRes requireContext(UUID userId, UUID personId, String contextType) {
        RelationshipType relationshipType = relationshipTypeForContext(contextType);
        UserProfile profile = userProfileRepository
                .findByUser_UserIdAndPerson_PersonIdAndRelationshipTypeAndActiveTrue(userId, personId, relationshipType)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Context is not allowed"));

        LoginRes.UserContextRes context = toContext(profile);
        if (!context.getContextType().equalsIgnoreCase(contextType)) {
            throw new org.springframework.security.access.AccessDeniedException("Context type is not allowed");
        }
        return context;
    }

    public boolean isContextStillValid(UUID userId, AuthToken token) {
        if (token.getActivePerson() == null || token.getActiveContextType() == null) {
            return true;
        }
        try {
            requireContext(userId, token.getActivePerson().getPersonId(), token.getActiveContextType());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public LoginRes.UserContextRes currentContext(AuthToken token) {
        Person person = token.getActivePerson();
        if (person == null || token.getActiveContextType() == null) {
            return null;
        }
        String displayName = person.getFullName() == null ? token.getActiveContextType() : person.getFullName();
        return new LoginRes.UserContextRes(
                person.getPersonId(),
                token.getActiveContextType(),
                null,
                userCodeFor(person),
                displayName
        );
    }

    public List<String> getAllFcmTokensByUserId(UUID userId) {
        List<AuthToken> tokens = authTokenRepository.findAllByUser_UserIdAndRevokedFalse(userId);
        return tokens.stream()
                .map(AuthToken::getFcmToken)
                .filter(fcmToken -> fcmToken != null && !fcmToken.isEmpty())
                .collect(Collectors.toList());
    }

    public List<String> getAllFcmTokensByActivePersonId(UUID personId) {
        List<AuthToken> tokens = authTokenRepository.findAllByActivePerson_PersonIdAndRevokedFalse(personId);
        return tokens.stream()
                .map(AuthToken::getFcmToken)
                .filter(fcmToken -> fcmToken != null && !fcmToken.isEmpty())
                .collect(Collectors.toList());
    }

    public List<AuthToken> getSessionsByUserId(UUID userId) {
        return authTokenRepository.findAllByUser_UserId(userId);
    }

    @Cacheable(value = "fcmTokensByRole", key = "#roleCode", unless = "#result == null || #result.isEmpty()", cacheManager = "redisCacheManager")
    public List<String> getAllFcmTokensByRoleCode(String roleCode) {
        List<UUID> userIds = userService.getAllUsersByRoleCode(roleCode).stream()
                .map(User::getUserId)
                .toList();

        if (userIds.isEmpty()) {
            return List.of();
        }

        return authTokenRepository.findAllByUser_UserIdInAndRevokedFalse(userIds).stream()
                .map(AuthToken::getFcmToken)
                .filter(fcmToken -> fcmToken != null && !fcmToken.isEmpty())
                .distinct()
                .toList();
    }

    @CacheEvict(value = "fcmTokensByRole", allEntries = true)
    public void deleteFcmTokenOnly(String fcmToken) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            return;
        }
        authTokenRepository.findByFcmToken(fcmToken).ifPresent(authToken -> {
            authToken.setFcmToken(null);
            authTokenRepository.save(authToken);
        });
    }

    private void revoke(AuthToken token) {
        token.setRevoked(true);
        token.setRevokedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now());
    }

    private void applyContext(AuthToken token, LoginRes.UserContextRes activeContext) {
        if (activeContext == null) {
            token.setActivePerson(null);
            token.setActiveContextType(null);
            return;
        }
        Person person = entityManager.getReference(Person.class, activeContext.getPersonId());
        token.setActivePerson(person);
        token.setActiveContextType(activeContext.getContextType());
    }

    private LoginRes.UserContextRes toContext(UserProfile profile) {
        Person person = profile.getPerson();
        String contextType = contextTypeFor(profile.getRelationshipType(), person);
        String displayName = person.getFullName() == null ? contextType : person.getFullName();
        return new LoginRes.UserContextRes(
                person.getPersonId(),
                contextType,
                profile.getRelationshipType().name(),
                userCodeFor(person),
                displayName
        );
    }

    private LoginRes.UserContextRes toContext(UserProfileRepository.UserContextRow row) {
        String contextType = contextTypeFor(row.getRelationshipType(), row.getStudentCode(), row.getStaffCode());
        String displayName = row.getDisplayName() == null ? contextType : row.getDisplayName();
        return new LoginRes.UserContextRes(
                row.getPersonId(),
                contextType,
                row.getRelationshipType(),
                userCodeFor(row.getStudentCode(), row.getStaffCode()),
                displayName
        );
    }

    private String userCodeFor(Person person) {
        if (person instanceof Student student) {
            return student.getStudentCode();
        }
        if (person instanceof Coach coach) {
            return coach.getStaffCode();
        }
        return null;
    }

    private String userCodeFor(String studentCode, String staffCode) {
        return studentCode != null ? studentCode : staffCode;
    }

    private String contextTypeFor(String relationshipType, String studentCode, String staffCode) {
        if (RelationshipType.GUARDIAN.name().equals(relationshipType)) {
            return "GUARDIAN";
        }
        if (RelationshipType.MANAGER.name().equals(relationshipType)) {
            return "MANAGER";
        }
        if (staffCode != null) {
            return "COACH";
        }
        if (studentCode != null) {
            return "STUDENT";
        }
        return relationshipType;
    }

    private String contextTypeFor(RelationshipType relationshipType, Person person) {
        if (relationshipType == RelationshipType.GUARDIAN) {
            return "GUARDIAN";
        }
        if (relationshipType == RelationshipType.MANAGER) {
            return "MANAGER";
        }
        if (person instanceof Coach) {
            return "COACH";
        }
        if (person instanceof Student) {
            return "STUDENT";
        }
        return relationshipType.name();
    }

    private RelationshipType relationshipTypeForContext(String contextType) {
        return switch (contextType.toUpperCase()) {
            case "GUARDIAN" -> RelationshipType.GUARDIAN;
            case "MANAGER" -> RelationshipType.MANAGER;
            default -> RelationshipType.OWNER;
        };
    }
}
