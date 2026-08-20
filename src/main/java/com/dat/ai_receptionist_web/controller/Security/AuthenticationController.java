package com.dat.ai_receptionist_web.controller.Security;

import com.dat.ai_receptionist_web.domain.Security.AuthToken;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Security.LoginReq;
import com.dat.ai_receptionist_web.dto.Security.LoginRes;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import com.dat.ai_receptionist_web.service.Security.AuthTokenService;
import com.dat.ai_receptionist_web.service.Security.AuthenticatedUserPrincipal;
import com.dat.ai_receptionist_web.service.Security.UserService;
import com.dat.ai_receptionist_web.util.PhoneNumberUtil;
import com.dat.ai_receptionist_web.util.RefreshTokenUtil;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String BAD_LOGIN_MESSAGE = "Số điện thoại hoặc mật khẩu không chính xác.";

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final AuthTokenService authTokenService;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    @Value("${auth.refresh-cookie.secure:false}")
    private boolean refreshCookieSecure;

    @Value("${auth.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    public AuthenticationController(AuthenticationManagerBuilder authenticationManagerBuilder,
                                    SecurityUtil securityUtil,
                                    UserService userService,
                                    AuthTokenService authTokenService) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.authTokenService = authTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginReq.UserBase loginReq) {
        String phoneNumber;
        AuthenticatedUserPrincipal principal;
        try {
            phoneNumber = PhoneNumberUtil.normalize(loginReq.getPhoneNumber());
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(phoneNumber, loginReq.getPassword());
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            principal = (AuthenticatedUserPrincipal) authentication.getPrincipal();
        } catch (BadCredentialsException | DisabledException | LockedException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(BAD_LOGIN_MESSAGE);
        }

        if (principal.getStatus() != UserStatus.ACTIVE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Tài khoản không ở trạng thái ACTIVE.");
        }

        UUID userId = principal.getUserId();
        List<LoginRes.UserContextRes> contexts = authTokenService.getActiveContexts(userId);
        if (contexts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Tài khoản chưa được liên kết hồ sơ.");
        }

        userService.updateLastLogin(userId);
        LoginRes.UserLogin userLogin = toUserLogin(principal);
        LoginRes.UserContextRes activeContext = contexts.size() == 1 ? contexts.getFirst() : null;

        String rawRefreshToken = RefreshTokenUtil.generateRawToken();
        AuthToken session = authTokenService.createSession(
                userId,
                RefreshTokenUtil.sha256(rawRefreshToken),
                loginReq.getIdDevice(),
                "WEB",
                loginReq.getFcmToken(),
                activeContext
        );

        String accessToken = securityUtil.createAccessToken(
                userId,
                session.getSessionId(),
                userLogin,
                activeContext
        );

        LoginRes response = new LoginRes();
        response.setAccessToken(accessToken);
        response.setIdDevice(loginReq.getIdDevice());
        response.setUser(userLogin);
        response.setActiveContext(activeContext);
        response.setAvailableContexts(contexts);
        response.setRequiresContextSelection(activeContext == null);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(rawRefreshToken).toString())
                .body(response);
    }

    @PostMapping("/mobile/login")
    public ResponseEntity<?> mobileLogin(@Valid @RequestBody LoginReq.MobileLoginRequest loginReq) {
        String phoneNumber;
        AuthenticatedUserPrincipal principal;
        try {
            phoneNumber = PhoneNumberUtil.normalize(loginReq.getPhoneNumber());
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(phoneNumber, loginReq.getPassword());
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            principal = (AuthenticatedUserPrincipal) authentication.getPrincipal();
        } catch (BadCredentialsException | DisabledException | LockedException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(BAD_LOGIN_MESSAGE);
        }

        if (principal.getStatus() != UserStatus.ACTIVE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Tài khoản không ở trạng thái ACTIVE.");
        }

        UUID userId = principal.getUserId();
        List<LoginRes.UserContextRes> contexts = authTokenService.getActiveContexts(userId);
        if (contexts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Tài khoản chưa được liên kết hồ sơ.");
        }

        userService.updateLastLogin(userId);
        LoginRes.UserLogin userLogin = toUserLogin(principal);
        LoginRes.UserContextRes activeContext = contexts.size() == 1 ? contexts.getFirst() : null;

        String rawRefreshToken = RefreshTokenUtil.generateRawToken();
        AuthToken session = authTokenService.createSession(
                userId,
                RefreshTokenUtil.sha256(rawRefreshToken),
                loginReq.getIdDevice(),
                loginReq.getPlatform().trim().toUpperCase(),
                loginReq.getFcmToken(),
                activeContext
        );

        String accessToken = securityUtil.createAccessToken(
                userId,
                session.getSessionId(),
                userLogin,
                activeContext
        );

        return ResponseEntity.ok(new LoginRes.MobileResponse(
                accessToken,
                rawRefreshToken,
                userLogin,
                activeContext,
                contexts,
                activeContext == null
        ));
    }

    @GetMapping("/account")
    public ResponseEntity<LoginRes> getAccount() {
        UUID userId = currentUserId();
        User user = userService.getUserWithRolesById(userId);
        String sessionId = SecurityUtil.getCurrentSessionId().orElse(null);
        AuthToken session = sessionId == null ? null : authTokenService.getBySessionId(sessionId);
        List<LoginRes.UserContextRes> contexts = authTokenService.getActiveContexts(userId);
        LoginRes.UserContextRes activeContext = resolveActiveContext(session, contexts);

        LoginRes response = new LoginRes();
        response.setUser(toUserLogin(user));
        response.setActiveContext(activeContext);
        response.setAvailableContexts(contexts);
        response.setRequiresContextSelection(activeContext == null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/contexts")
    public ResponseEntity<List<LoginRes.UserContextRes>> contexts() {
        return ResponseEntity.ok(authTokenService.getActiveContexts(currentUserId()));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionRes>> sessions() {
        UUID userId = currentUserId();
        List<SessionRes> sessions = authTokenService.getSessionsByUserId(userId).stream()
                .sorted(Comparator.comparing(AuthToken::getCreatedAt).reversed())
                .map(SessionRes::from)
                .toList();
        return ResponseEntity.ok(sessions);
    }

    @PostMapping("/switch-context")
    public ResponseEntity<LoginRes> switchContext(@Valid @RequestBody LoginReq.SwitchContextReq req) {
        UUID userId = currentUserId();
        String sessionId = SecurityUtil.getCurrentSessionId()
                .orElseThrow(() -> new AccessDeniedException("Missing session"));
        AuthTokenService.ContextSwitchResult switchResult = authTokenService.switchContext(
                userId,
                sessionId,
                UUID.fromString(req.getPersonId()),
                req.getContextType()
        );
        LoginRes.UserContextRes context = switchResult.activeContext();
        List<LoginRes.UserContextRes> contexts = switchResult.availableContexts();

        User user = userService.getUserWithRolesById(userId);
        LoginRes.UserLogin userLogin = toUserLogin(user);
        LoginRes response = new LoginRes();
        response.setAccessToken(securityUtil.createAccessToken(userId, sessionId, userLogin, context));
        response.setUser(userLogin);
        response.setActiveContext(context);
        response.setAvailableContexts(contexts);
        response.setRequiresContextSelection(false);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginRes> refresh(@CookieValue(name = REFRESH_COOKIE, defaultValue = "") String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return unauthorizedWithDeletedCookie();
        }

        AuthToken current = authTokenService.getByRefreshTokenHash(refreshToken);
        if (current == null || !isRefreshable(current)) {
            return unauthorizedWithDeletedCookie();
        }

        String newRawRefreshToken = RefreshTokenUtil.generateRawToken();
        AuthToken rotated;
        try {
            rotated = authTokenService.rotateRefreshToken(refreshToken, newRawRefreshToken);
        } catch (RuntimeException e) {
            return unauthorizedWithDeletedCookie();
        }

        User user = userService.getUserWithRolesById(rotated.getUser().getUserId());
        if (user.getStatus() != UserStatus.ACTIVE) {
            authTokenService.revokeBySessionId(rotated.getSessionId());
            return unauthorizedWithDeletedCookie();
        }

        List<LoginRes.UserContextRes> contexts = authTokenService.getActiveContexts(user.getUserId());
        LoginRes.UserContextRes activeContext = resolveActiveContext(rotated, contexts);
        if (hasStoredContext(rotated) && activeContext == null) {
            authTokenService.updateContext(rotated.getSessionId(), null);
        }

        LoginRes.UserLogin userLogin = toUserLogin(user);
        LoginRes response = new LoginRes();
        response.setAccessToken(securityUtil.createAccessToken(user.getUserId(), rotated.getSessionId(), userLogin, activeContext));
        response.setUser(userLogin);
        response.setActiveContext(activeContext);
        response.setAvailableContexts(contexts);
        response.setRequiresContextSelection(activeContext == null);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(newRawRefreshToken).toString())
                .body(response);
    }

    @PostMapping("/mobile/refresh")
    public ResponseEntity<LoginRes.MobileResponse> mobileRefresh(
            @Valid @RequestBody LoginReq.RefreshTokenRequest request
    ) {
        String refreshToken = request.getRefreshToken();
        AuthToken current = authTokenService.getByRefreshTokenHash(refreshToken);
        if (current == null || !isRefreshable(current)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String newRawRefreshToken = RefreshTokenUtil.generateRawToken();
        AuthToken rotated;
        try {
            rotated = authTokenService.rotateRefreshToken(refreshToken, newRawRefreshToken);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.getUserWithRolesById(rotated.getUser().getUserId());
        if (user.getStatus() != UserStatus.ACTIVE) {
            authTokenService.revokeBySessionId(rotated.getSessionId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<LoginRes.UserContextRes> contexts = authTokenService.getActiveContexts(user.getUserId());
        LoginRes.UserContextRes activeContext = resolveActiveContext(rotated, contexts);
        if (hasStoredContext(rotated) && activeContext == null) {
            authTokenService.updateContext(rotated.getSessionId(), null);
        }

        LoginRes.UserLogin userLogin = toUserLogin(user);
        String accessToken = securityUtil.createAccessToken(
                user.getUserId(),
                rotated.getSessionId(),
                userLogin,
                activeContext
        );

        return ResponseEntity.ok(new LoginRes.MobileResponse(
                accessToken,
                newRawRefreshToken,
                userLogin,
                activeContext,
                contexts,
                activeContext == null
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, defaultValue = "") String refreshToken) {
        authTokenService.revokeByRawRefreshToken(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString())
                .build();
    }

    @PostMapping("/mobile/logout")
    public ResponseEntity<Void> mobileLogout(
            @Valid @RequestBody LoginReq.RefreshTokenRequest request
    ) {
        authTokenService.revokeByRawRefreshToken(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        authTokenService.revokeAllByUserId(currentUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString())
                .build();
    }

    @PostMapping("/update-fcm")
    public ResponseEntity<Void> updateFcmToken(@Valid @RequestBody LoginReq.UpdateFcmReq req) {
        String sessionId = SecurityUtil.getCurrentSessionId()
                .orElseThrow(() -> new AccessDeniedException("Missing session"));
        authTokenService.updateFcmTokenForSession(sessionId, req.getFcmToken(), req.getPlatform());
        return ResponseEntity.ok().build();
    }

    private LoginRes.UserLogin toUserLogin(AuthenticatedUserPrincipal principal) {
        Set<String> roles = principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
        return new LoginRes.UserLogin(principal.getUserId(), principal.getUsername(), principal.getStatus(), roles);
    }

    private LoginRes.UserLogin toUserLogin(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getCode())
                .collect(Collectors.toSet());
        return new LoginRes.UserLogin(user.getUserId(), user.getPhoneNumber(), user.getStatus(), roles);
    }

    private UUID currentUserId() {
        return SecurityUtil.getCurrentUserId()
                .map(UUID::fromString)
                .orElseThrow(() -> new AccessDeniedException("Missing user id"));
    }

    private LoginRes.UserContextRes resolveActiveContext(AuthToken session, List<LoginRes.UserContextRes> contexts) {
        if (!hasStoredContext(session)) {
            return null;
        }

        UUID activePersonId = session.getActivePerson().getPersonId();
        String activeContextType = session.getActiveContextType();
        return contexts.stream()
                .filter(context -> context.getPersonId().equals(activePersonId)
                        && context.getContextType().equalsIgnoreCase(activeContextType))
                .findFirst()
                .orElse(null);
    }

    private boolean hasStoredContext(AuthToken session) {
        return session != null
                && session.getActivePerson() != null
                && session.getActiveContextType() != null;
    }

    private boolean isRefreshable(AuthToken token) {
        return !token.isRevoked()
                && token.getExpiresAt() != null
                && token.getExpiresAt().isAfter(LocalDateTime.now());
    }

    private ResponseEntity<LoginRes> unauthorizedWithDeletedCookie() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString())
                .build();
    }

    private ResponseCookie refreshCookie(String rawRefreshToken) {
        return ResponseCookie.from(REFRESH_COOKIE, rawRefreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path("/api/v1/auth")
                .sameSite(refreshCookieSameSite)
                .maxAge(refreshTokenExpiration)
                .build();
    }

    private ResponseCookie deleteRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path("/api/v1/auth")
                .sameSite(refreshCookieSameSite)
                .maxAge(0)
                .build();
    }

    public record SessionRes(String sessionId, String deviceInfo, String platform, boolean revoked,
                             LocalDateTime createdAt, LocalDateTime lastUsedAt, LocalDateTime expiresAt,
                             String activeContextType) {
        static SessionRes from(AuthToken token) {
            return new SessionRes(
                    token.getSessionId(),
                    token.getDeviceInfo(),
                    token.getPlatform() == null ? "WEB" : token.getPlatform(),
                    token.isRevoked(),
                    token.getCreatedAt(),
                    token.getLastUsedAt(),
                    token.getExpiresAt(),
                    token.getActiveContextType()
            );
        }
    }
}
