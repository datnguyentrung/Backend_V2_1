package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.domain.Security.*;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.AuthSessionDTO;
import com.dat.ai_receptionist_web.dto.Security.LoginRes;
import com.dat.ai_receptionist_web.mapper.Security.AuthSessionMapper;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.*;
import com.dat.ai_receptionist_web.util.RefreshTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthSessionService {
    private final AuthSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final UserPersonRepository userPersonRepository;
    private final AuthSessionMapper authSessionMapper;

    public AuthSessionService(AuthSessionRepository sessionRepository, UserRepository userRepository,
                              UserPersonRepository userPersonRepository) {
        this(sessionRepository, userRepository, userPersonRepository, new AuthSessionMapper());
    }

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenValidity;

    @Transactional(readOnly = true)
    public PageResponse<AuthSessionDTO.Response> list(Pageable pageable) {
        return PageResponse.of(sessionRepository.findAll(pageable), authSessionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AuthSessionDTO.Response get(UUID id) {
        return authSessionMapper.toResponse(find(id));
    }

    @Transactional
    public AuthSessionDTO.Response create(AuthSessionDTO.CreateRequest request) {
        AuthSession session = new AuthSession();
        session.setUser(userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found")));
        session.setActiveUserPerson(request.activeUserPersonId() == null ? null
                : userPersonRepository.findById(request.activeUserPersonId())
                .orElseThrow(() -> new IllegalArgumentException("UserPerson not found")));
        session.setRefreshTokenHash(request.refreshTokenHash());
        session.setDeviceInfo(request.deviceInfo());
        session.setPlatform(request.platform());
        session.setFcmToken(request.fcmToken());
        session.setExpiresAt(request.expiresAt());
        session.setRevoked(request.revoked());
        session.setRevokedAt(request.revokedAt());
        session.setVersion(request.version());
        return authSessionMapper.toResponse(sessionRepository.save(session));
    }

    @Transactional
    public AuthSessionDTO.Response update(UUID id, AuthSessionDTO.UpdateRequest request) {
        AuthSession session = find(id);
        session.setUser(userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found")));
        session.setActiveUserPerson(request.activeUserPersonId() == null ? null
                : userPersonRepository.findById(request.activeUserPersonId())
                .orElseThrow(() -> new IllegalArgumentException("UserPerson not found")));
        authSessionMapper.updateEntity(request, session);
        return authSessionMapper.toResponse(sessionRepository.save(session));
    }

    @Transactional
    public void delete(UUID id) {
        revoke(find(id));
    }

    @Transactional
    public AuthSession create(UUID userId, String rawRefreshToken, String deviceInfo,
                              String platform, String fcmToken, UUID activeUserPersonId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserPerson active = activeUserPersonId == null ? null
                : userPersonRepository.findByUserPersonIdAndUser_UserIdAndActiveTrue(activeUserPersonId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Person context is not owned by user"));
        return sessionRepository.save(AuthSession.builder()
                .user(user)
                .refreshTokenHash(RefreshTokenUtil.sha256(rawRefreshToken))
                .deviceInfo(deviceInfo)
                .platform(platform)
                .fcmToken(fcmToken)
                .activeUserPerson(active)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenValidity))
                .revoked(false)
                .build());
    }

    @Transactional
    public AuthSession rotate(String rawCurrentToken, String rawNewToken) {
        AuthSession session = sessionRepository
                .findByRefreshTokenHashForUpdate(RefreshTokenUtil.sha256(rawCurrentToken))
                .orElseThrow(() -> new IllegalArgumentException("Refresh token is invalid"));
        if (session.isRevoked() || !session.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh session is not active");
        }
        session.setRefreshTokenHash(RefreshTokenUtil.sha256(rawNewToken));
        session.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenValidity));
        return session;
    }

    @Transactional
    public void revokeByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        sessionRepository.findByRefreshTokenHash(RefreshTokenUtil.sha256(rawToken)).ifPresent(this::revoke);
    }

    @Transactional
    public void revoke(UUID sessionId) {
        sessionRepository.findById(sessionId).ifPresent(this::revoke);
    }

    @Transactional
    public void revokeAll(UUID userId) {
        sessionRepository.findAllByUser_UserIdAndRevokedFalse(userId).forEach(this::revoke);
    }

    @Transactional
    public ContextSwitchResult switchContext(UUID userId, UUID sessionId, UUID userPersonId) {
        AuthSession session = sessionRepository.findById(sessionId)
                .filter(value -> value.getUser().getUserId().equals(userId) && !value.isRevoked())
                .orElseThrow(() -> new IllegalArgumentException("Session is not active"));
        UserPerson target = userPersonRepository
                .findByUserPersonIdAndUser_UserIdAndActiveTrue(userPersonId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Person context is not owned by user"));
        session.setActiveUserPerson(target);
        List<LoginRes.UserContextRes> available = contexts(userId);
        return new ContextSwitchResult(toContext(target), available);
    }

    @Transactional(readOnly = true)
    public List<LoginRes.UserContextRes> contexts(UUID userId) {
        return userPersonRepository.findAllByUser_UserIdAndActiveTrue(userId).stream()
                .map(this::toContext).toList();
    }

    @Transactional(readOnly = true)
    public List<AuthSession> sessions(UUID userId) {
        return sessionRepository.findAllByUser_UserId(userId);
    }

    @Transactional
    public void updateFcm(UUID sessionId, String token, String platform) {
        AuthSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        session.setFcmToken(token);
        if (platform != null) session.setPlatform(platform.toUpperCase(Locale.ROOT));
    }

    @Transactional(readOnly = true)
    public Set<String> fcmTokensForUser(UUID userId) {
        Set<String> result = new HashSet<>();
        sessionRepository.findAllByUser_UserIdAndRevokedFalse(userId).stream()
                .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(AuthSession::getFcmToken).filter(Objects::nonNull).forEach(result::add);
        return result;
    }

    private void revoke(AuthSession session) {
        if (!session.isRevoked()) {
            session.setRevoked(true);
            session.setRevokedAt(LocalDateTime.now());
        }
    }

    private AuthSession find(UUID id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AuthSession not found"));
    }

    private LoginRes.UserContextRes toContext(UserPerson value) {
        return new LoginRes.UserContextRes(value.getUserPersonId(), value.getPerson().getPersonId(),
                value.getRelationshipType(), value.getPerson().getPersonCode(), value.getPerson().getFullName());
    }

    public record ContextSwitchResult(LoginRes.UserContextRes activeContext,
                                      List<LoginRes.UserContextRes> availableContexts) {
    }
}
