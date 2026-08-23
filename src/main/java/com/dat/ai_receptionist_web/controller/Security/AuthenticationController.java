package com.dat.ai_receptionist_web.controller.Security;

import com.dat.ai_receptionist_web.domain.Security.AuthSession;
import com.dat.ai_receptionist_web.dto.Security.*;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import com.dat.ai_receptionist_web.service.Security.*;
import com.dat.ai_receptionist_web.util.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final AuthorizationService authorizationService;
    private final AuthSessionService sessionService;
    private final SecurityUtil securityUtil;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;
    @Value("${auth.refresh-cookie.secure:true}")
    private boolean refreshCookieSecure;
    @Value("${auth.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    @PostMapping("/login")
    public ResponseEntity<LoginRes> login(@Valid @RequestBody LoginReq.UserBase request) {
        LoginBundle bundle = authenticate(request, "WEB");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(bundle.refreshToken()).toString())
                .body(bundle.response());
    }

    @PostMapping("/mobile/login")
    public ResponseEntity<LoginRes.MobileResponse> mobileLogin(
            @Valid @RequestBody LoginReq.MobileLoginRequest request) {
        LoginBundle bundle = authenticate(request, request.getPlatform().toUpperCase(Locale.ROOT));
        return ResponseEntity.ok(toMobile(bundle.response(), bundle.refreshToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginRes> refresh(
            @CookieValue(name = REFRESH_COOKIE, defaultValue = "") String rawToken) {
        try {
            LoginBundle bundle = refreshBundle(rawToken);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie(bundle.refreshToken()).toString())
                    .body(bundle.response());
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
        }
    }

    @PostMapping("/mobile/refresh")
    public ResponseEntity<LoginRes.MobileResponse> mobileRefresh(
            @Valid @RequestBody LoginReq.RefreshTokenRequest request) {
        try {
            LoginBundle bundle = refreshBundle(request.getRefreshToken());
            return ResponseEntity.ok(toMobile(bundle.response(), bundle.refreshToken()));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/account")
    public LoginRes account() {
        UUID userId = currentUserId();
        AuthorizationSnapshot snapshot = authorizationService.loadSnapshot(userId);
        UUID sessionId = currentSessionId();
        List<LoginRes.UserContextRes> contexts = sessionService.contexts(userId);
        LoginRes.UserContextRes active = contextById(contexts,
                SecurityUtil.getCurrentActiveUserPersonId().orElse(null));
        return response(null, null, snapshot, active, contexts);
    }

    @GetMapping("/contexts")
    public List<LoginRes.UserContextRes> contexts() {
        return sessionService.contexts(currentUserId());
    }

    @PostMapping("/switch-context")
    public LoginRes switchContext(@Valid @RequestBody LoginReq.SwitchContextReq request) {
        UUID userId = currentUserId();
        UUID sessionId = currentSessionId();
        AuthSessionService.ContextSwitchResult switched =
                sessionService.switchContext(userId, sessionId, request.getUserPersonId());
        AuthorizationSnapshot snapshot = authorizationService.loadSnapshot(userId);
        return response(securityUtil.createAccessToken(sessionId, snapshot, request.getUserPersonId()),
                null, snapshot, switched.activeContext(), switched.availableContexts());
    }

    @GetMapping("/sessions")
    public List<SessionRes> sessions() {
        return sessionService.sessions(currentUserId()).stream()
                .sorted(Comparator.comparing(AuthSession::getCreatedAt).reversed())
                .map(SessionRes::from).toList();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, defaultValue = "") String rawToken) {
        sessionService.revokeByRawToken(rawToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
    }

    @PostMapping("/mobile/logout")
    public void mobileLogout(@Valid @RequestBody LoginReq.RefreshTokenRequest request) {
        sessionService.revokeByRawToken(request.getRefreshToken());
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        sessionService.revokeAll(currentUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
    }

    @PostMapping("/update-fcm")
    public void updateFcm(@Valid @RequestBody LoginReq.UpdateFcmReq request) {
        sessionService.updateFcm(currentSessionId(), request.getFcmToken(), request.getPlatform());
    }

    private LoginBundle authenticate(LoginReq.UserBase request, String platform) {
        Authentication authenticated;
        try {
            authenticated = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    PhoneNumberUtil.normalize(request.getPhoneNumber()), request.getPassword()));
        } catch (BadCredentialsException | DisabledException | LockedException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Invalid phone number or password");
        }
        AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) authenticated.getPrincipal();
        if (principal.getStatus() != UserStatus.ACTIVE) throw new DisabledException("User is not active");

        UUID userId = principal.getUserId();
        AuthorizationSnapshot snapshot = authorizationService.loadSnapshot(userId);
        List<LoginRes.UserContextRes> contexts = sessionService.contexts(userId);
        LoginRes.UserContextRes active = contexts.size() == 1 ? contexts.getFirst() : null;
        String rawRefreshToken = RefreshTokenUtil.generateRawToken();
        AuthSession session = sessionService.create(userId, rawRefreshToken, request.getIdDevice(),
                platform, request.getFcmToken(), active == null ? null : active.userPersonId());
        userService.updateLastLogin(userId);
        String accessToken = securityUtil.createAccessToken(session.getAuthSessionId(), snapshot,
                active == null ? null : active.userPersonId());
        return new LoginBundle(response(accessToken, request.getIdDevice(), snapshot, active, contexts),
                rawRefreshToken);
    }

    private LoginBundle refreshBundle(String currentRawToken) {
        if (currentRawToken == null || currentRawToken.isBlank()) {
            throw new IllegalArgumentException("Missing refresh token");
        }
        String nextRawToken = RefreshTokenUtil.generateRawToken();
        AuthSession session = sessionService.rotate(currentRawToken, nextRawToken);
        UUID userId = session.getUser().getUserId();
        AuthorizationSnapshot snapshot = authorizationService.loadSnapshot(userId);
        if (snapshot.userStatus() != UserStatus.ACTIVE) {
            sessionService.revoke(session.getAuthSessionId());
            throw new IllegalArgumentException("User is not active");
        }
        List<LoginRes.UserContextRes> contexts = sessionService.contexts(userId);
        UUID activeId = session.getActiveUserPerson() == null
                ? null : session.getActiveUserPerson().getUserPersonId();
        LoginRes.UserContextRes active = contextById(contexts, activeId);
        if (activeId != null && active == null) {
            sessionService.revoke(session.getAuthSessionId());
            throw new IllegalArgumentException("Active context is no longer available");
        }
        String accessToken = securityUtil.createAccessToken(session.getAuthSessionId(), snapshot, activeId);
        return new LoginBundle(response(accessToken, session.getDeviceInfo(), snapshot, active, contexts),
                nextRawToken);
    }

    private LoginRes response(String token, String device, AuthorizationSnapshot snapshot,
                              LoginRes.UserContextRes active, List<LoginRes.UserContextRes> contexts) {
        LoginRes result = new LoginRes();
        result.setAccessToken(token);
        result.setIdDevice(device);
        result.setUser(new LoginRes.UserLogin(snapshot.userId(), snapshot.phoneNumber(),
                snapshot.userStatus(), snapshot.roleCodes(), snapshot.permissionCodes()));
        result.setActiveContext(active);
        result.setAvailableContexts(contexts);
        result.setRequiresContextSelection(active == null);
        return result;
    }

    private LoginRes.MobileResponse toMobile(LoginRes response, String refreshToken) {
        return new LoginRes.MobileResponse(response.getAccessToken(), refreshToken, response.getUser(),
                response.getActiveContext(), response.getAvailableContexts(),
                response.isRequiresContextSelection());
    }

    private LoginRes.UserContextRes contextById(List<LoginRes.UserContextRes> contexts, UUID id) {
        if (id == null) return null;
        return contexts.stream().filter(value -> value.userPersonId().equals(id)).findFirst().orElse(null);
    }

    private UUID currentUserId() {
        return SecurityUtil.getCurrentUserId().orElseThrow(() -> new AccessDeniedException("Missing user"));
    }

    private UUID currentSessionId() {
        return SecurityUtil.getCurrentSessionId().orElseThrow(() -> new AccessDeniedException("Missing session"));
    }

    private ResponseCookie refreshCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value).httpOnly(true).secure(refreshCookieSecure)
                .path("/api/v1/auth").sameSite(refreshCookieSameSite)
                .maxAge(refreshTokenExpiration).build();
    }

    private ResponseCookie deleteRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "").httpOnly(true).secure(refreshCookieSecure)
                .path("/api/v1/auth").sameSite(refreshCookieSameSite).maxAge(0).build();
    }

    private record LoginBundle(LoginRes response, String refreshToken) {
    }

    public record SessionRes(UUID authSessionId, String deviceInfo, String platform, boolean revoked,
                             LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime expiresAt,
                             UUID activeUserPersonId) {
        static SessionRes from(AuthSession session) {
            return new SessionRes(session.getAuthSessionId(), session.getDeviceInfo(), session.getPlatform(),
                    session.isRevoked(), session.getCreatedAt(), session.getUpdatedAt(), session.getExpiresAt(),
                    session.getActiveUserPerson() == null ? null
                            : session.getActiveUserPerson().getUserPersonId());
        }
    }
}
