package com.dat.backend_v2_1.controller.Security;

import com.dat.backend_v2_1.domain.Security.AuthToken;
import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.dto.Security.LoginReq;
import com.dat.backend_v2_1.dto.Security.LoginRes;
import com.dat.backend_v2_1.service.Security.AuthTokenService;
import com.dat.backend_v2_1.service.Security.UserService;
import com.dat.backend_v2_1.util.SecurityUtil;
import com.dat.backend_v2_1.util.error.AuthenticationException;
import com.dat.backend_v2_1.util.error.IdInvalidException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final AuthTokenService authTokenService;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    public AuthenticationController(
            AuthenticationManagerBuilder authenticationManagerBuilder,
            SecurityUtil securityUtil,
            UserService userService,
            AuthTokenService userTokensService) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.authTokenService = userTokensService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginRes> login(@Valid @RequestBody LoginReq.UserBase loginReq) throws NoSuchAlgorithmException {
        // Nạp input gồm username/passwork vào Security
        UsernamePasswordAuthenticationToken authenticationToken
                = new UsernamePasswordAuthenticationToken(loginReq.getPhoneNumber(), loginReq.getPassword());

        // Xác thực người dùng => cần viết hàm loadUserByUsername
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // Set thông tin người dùng đăng nhập vào context (có thể sử dụng sau này)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        LoginRes loginRes = new LoginRes();
        User currentUserDB = userService.getUserByPhoneNumber(loginReq.getPhoneNumber());

        if (currentUserDB != null) {
            LoginRes.UserLogin userLogin = new LoginRes.UserLogin(
                    // .get phải thứ tự UserLogin
                    currentUserDB.getUserId(),
                    currentUserDB.getStatus(),
                    currentUserDB.getRole().getCode(),
                    currentUserDB.getCreatedAt().toString()
            );
            loginRes.setUser(userLogin);
        }

        // Lưu mã thiết bị
        loginRes.setIdDevice(loginReq.getIdDevice());

        // Create access token
        assert currentUserDB != null;
        String accessToken = securityUtil.createAccessToken(currentUserDB.getUserId(), loginRes.getUser());
        loginRes.setAccessToken(accessToken);

        // Create refresh token
        String refreshToken = UUID.randomUUID() + "-" + SecureRandom.getInstanceStrong().nextLong();
        loginRes.setRefreshToken(refreshToken);

        // update user
        authTokenService.updateUserTokens(
                refreshToken,
                currentUserDB.getUserId().toString(),
                loginReq.getIdDevice(),
                loginReq.getFcmToken()
        );

        ResponseCookie responseCookie = ResponseCookie
                .from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(loginRes);
    }

    @GetMapping("/account")
    public ResponseEntity<LoginRes.UserLogin> getAccount() {
        String phoneNumber = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : null;
        User currentUserDB = userService.getUserByPhoneNumber(phoneNumber);
        LoginRes.UserLogin userLogin = new LoginRes.UserLogin();
        if (currentUserDB != null) {
            userLogin.setUserId(currentUserDB.getUserId());
            userLogin.setStatus(currentUserDB.getStatus());
            userLogin.setRole(currentUserDB.getRole().getCode());
        }
        return ResponseEntity.ok().body(userLogin);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginRes> getRefreshToken(
            @CookieValue(name = "refresh_token", defaultValue = "") String refreshToken
    ) throws AuthenticationException, IdInvalidException, NoSuchAlgorithmException {
        if (refreshToken == null || refreshToken.isEmpty()) {
//            throw new IdInvalidException("Bạn không có refresh token ở cookies");
            throw new IdInvalidException("Bạn không có refresh token");
        }

        // 1. DÙNG DATABASE ĐỂ TRA CỨU THAY VÌ DECODE JWT
        AuthToken currentUserDB = authTokenService.getUserTokenByRefreshToken(refreshToken);

        if (currentUserDB == null) {
            throw new AuthenticationException("Token không hợp lệ hoặc thiết bị không khớp");
        }

        // 2. Kiểm tra token đã hết hạn hay chưa
        if (currentUserDB.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthenticationException("Token đã hết hạn");
        }
        if (currentUserDB.isRevoked()) {
            authTokenService.logoutUserTokens(
                    currentUserDB.getUser().getUserId().toString(),
                    currentUserDB.getDeviceInfo());
            throw new AuthenticationException("Phát hiện truy cập bất thường. Phiên đăng nhập bị khóa.");
        }

        String idUser = currentUserDB.getUser().getUserId().toString();
        String idDevice = currentUserDB.getDeviceInfo();
        User userDB = currentUserDB.getUser();

        // issue new token/set refresh token as cookies
        LoginRes loginRes = new LoginRes();
        loginRes.setIdDevice(idDevice);

        LoginRes.UserLogin userLogin = new LoginRes.UserLogin(
                userDB.getUserId(),
                userDB.getStatus(),
                userDB.getRole().getCode(),
                userDB.getCreatedAt().toString()
        );
        loginRes.setUser(userLogin);

        // 5. Tạo Access Token mới (vẫn là JWT)
        String accessToken = securityUtil.createAccessToken(userDB.getUserId(), loginRes.getUser());
        loginRes.setAccessToken(accessToken);

        // 6. Tạo Refresh Token MỚI (Opaque Token)
        String new_refreshToken = UUID.randomUUID() + "-" + SecureRandom.getInstanceStrong().nextLong();
        loginRes.setRefreshToken(new_refreshToken);

        // update user
        authTokenService.updateUserTokens(new_refreshToken, idUser, idDevice, null);

        // set cookies
        ResponseCookie responseCookie = ResponseCookie
                .from("refresh_token", new_refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(loginRes);
//        return ResponseEntity.ok(loginRes);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", defaultValue = "") String refreshToken
    ) throws IdInvalidException, AuthenticationException {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IdInvalidException("Không tìm thấy refresh token");
        }

        AuthToken currentUserDB = authTokenService.getUserTokenByRefreshToken(refreshToken);

        if (currentUserDB != null) {
            String idUser = currentUserDB.getUser().getUserId().toString();
            String idDevice = currentUserDB.getDeviceInfo();

            authTokenService.logoutUserTokens(idUser, idDevice);
        }

        // Xóa cookie refresh token trên client
        ResponseCookie deleteSpringCookie = ResponseCookie
                .from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteSpringCookie.toString())
                .build();
    }

    @PostMapping("/update-fcm")
    public ResponseEntity<?> updateFcmToken(@Valid @RequestBody LoginReq.UpdateFcmReq req) {
        try {
            authTokenService.updateFcmTokenOnly(req.getRefreshToken(), req.getFcmToken());
            return ResponseEntity.ok("Cập nhật FCM Token thành công");
        } catch (RuntimeException e) {
            // Xử lý nếu không tìm thấy token
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
