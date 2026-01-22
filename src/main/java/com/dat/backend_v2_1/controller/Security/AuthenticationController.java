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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<LoginRes> login(@Valid @RequestBody LoginReq.UserBase loginReq) {
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
                    currentUserDB.getPhoneNumber(),
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
        String refreshToken = securityUtil.createRefreshToken(currentUserDB.getUserId(), loginRes);
        loginRes.setRefreshToken(refreshToken);

        // update user
        authTokenService.updateUserTokens(refreshToken, currentUserDB.getUserId().toString(), loginReq.getIdDevice());

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
            userLogin.setIdAccount(currentUserDB.getPhoneNumber());
            userLogin.setStatus(currentUserDB.getStatus());
            userLogin.setRole(currentUserDB.getRole().getCode());
        }
        return ResponseEntity.ok().body(userLogin);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginRes> getRefreshToken(
//            @CookieValue(name = "refresh_token", defaultValue = "") String refreshToken
        @RequestBody LoginReq.RefreshRequest request
    ) throws AuthenticationException, IdInvalidException {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
//            throw new IdInvalidException("Bạn không có refresh token ở cookies");
            throw new IdInvalidException("Bạn không có refresh token");
        }

        Jwt decodedToken = securityUtil.checkValidRefreshToken(refreshToken);
        String idUser = decodedToken.getSubject();
        String idDevice = decodedToken.getClaim("id_device");

        // check user by token + phoneNumber + idDevice
        AuthToken currentUserDB = authTokenService
                .getUserTokensByRefreshTokenAndIdAccountAndIdDevice(refreshToken, idUser, idDevice);
        if (currentUserDB == null) {
            throw new AuthenticationException("Token không hợp lệ hoặc thiết bị không khớp");
        }

        // issue new token/set refresh token as cookies
        LoginRes loginRes = new LoginRes();
        User userDB = userService.getUserById(idUser);
        System.out.println("UserDB: " + userDB);
        if (userDB != null) {
            System.out.println("IdAccount: " + userDB.getPhoneNumber());
            System.out.println("Status: " + userDB.getStatus());
            System.out.println("Role ID: " + (userDB.getRole() != null ? userDB.getRole().getCode() : "NULL ROLE OBJECT")); // Kiểm tra kỹ Role
            System.out.println("CreatedAt: " + userDB.getCreatedAt());
            LoginRes.UserLogin userLogin = new LoginRes.UserLogin(
                    userDB.getPhoneNumber(),
                    userDB.getStatus(),
                    userDB.getRole().getCode(),
                    userDB.getCreatedAt().toString()
            );
            loginRes.setUser(userLogin);
        }
        loginRes.setIdDevice(idDevice);

        // Create access token
        assert userDB != null;
        String accessToken = securityUtil.createAccessToken(userDB.getUserId(), loginRes.getUser());
        loginRes.setAccessToken(accessToken);

        // Create refresh token
        String new_refreshToken = securityUtil.createRefreshToken(userDB.getUserId(), loginRes);

        loginRes.setRefreshToken(new_refreshToken);

        // update user
        authTokenService.updateUserTokens(new_refreshToken, idUser, idDevice);

        // set cookies
//        ResponseCookie responseCookie = ResponseCookie
//                .from("refresh_token", refreshToken)
//                .httpOnly(true)
//                .secure(true)
//                .path("/")
//                .maxAge(refreshTokenExpiration)
//                .build();
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
//                .body(loginRes);
        return ResponseEntity.ok(loginRes);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
//            @CookieValue(name = "refresh_token", defaultValue = "") String refreshToken
            @RequestBody LoginReq.RefreshRequest request
    ) throws IdInvalidException, AuthenticationException {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
//            throw new IdInvalidException("Không tìm thấy refresh token trong cookie");
            throw new IdInvalidException("Không tìm thấy refresh token");
        }

        Jwt decodedToken = securityUtil.checkValidRefreshToken(refreshToken);

        String idUser = decodedToken.getSubject();
        // Lấy claim "user" và convert về UserLogin
        Object userObj = decodedToken.getClaim("user");

        // Lấy claim "id_device" an toàn
        String idDevice = decodedToken.getClaim("id_device");
        System.out.println("idDevice: " + idDevice);

        // update refresh token = null
        authTokenService.updateUserTokens(null, idUser, idDevice);

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
}
