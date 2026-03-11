package com.dat.backend_v2_1.service.Security;

import com.dat.backend_v2_1.domain.Security.AuthToken;
import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.repository.Security.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private final AuthTokenRepository authTokenRepository;

    private final UserService userService;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    public AuthToken getUserTokensByIdUserAndDevice(String idUserStr, String idDevice) {
        UUID userId;
        try {
            userId = UUID.fromString(idUserStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("User ID không đúng định dạng UUID");
        }

        return authTokenRepository.findByUser_UserIdAndDeviceInfo(userId, idDevice)
                .orElse(null);
    }

    public void logoutUserTokens(String idUserStr, String idDevice) {
        UUID userId;
        try {
            userId = UUID.fromString(idUserStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("User ID không đúng định dạng UUID"); // Hoặc dùng Custom Exception
        }

        // Tìm token
        AuthToken existingToken = authTokenRepository.findByUser_UserIdAndDeviceInfo(userId, idDevice)
                .orElse(null);

        // Chỉ xử lý và lưu nếu token tồn tại
        if (existingToken != null) {
            // 1. Đánh dấu đã thu hồi
            existingToken.setRevoked(true);

            // 2. Set thời gian hết hạn về HIỆN TẠI (để coi như nó đã chết ngay lập tức)
            // Đừng cộng thêm thời gian!
            existingToken.setExpiresAt(Instant.now());

            // 3. Lưu lại (Phải nằm TRONG khối if)
            authTokenRepository.save(existingToken);
        }
        // Nếu token == null, tức là user đã logout rồi hoặc token không tồn tại.
        // Ta có thể bỏ qua (return) mà không cần báo lỗi.
    }

    public void updateUserTokens(String token, String idUser, String idDevice, String fcmToken) {
        UUID userId;
        try {
            userId = UUID.fromString(idUser);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("User ID không đúng định dạng UUID");
        }

        AuthToken currentUser = authTokenRepository.findByUser_UserIdAndDeviceInfo(userId, idDevice)
                .orElse(null);

        if (currentUser != null) {
            // Update existing token
            currentUser.setRefreshToken(token);
            currentUser.setExpiresAt(Instant.now().plusSeconds(refreshTokenExpiration));
            currentUser.setRevoked(false);

            // Nếu client có gửi fcmToken lên thì mới update, không thì giữ nguyên cái cũ
            if (fcmToken != null && !fcmToken.isEmpty()) {
                currentUser.setFcmToken(fcmToken);
            }

            authTokenRepository.save(currentUser);
        } else {
            // Create new token with refreshToken
            User user = userService.getUserById(userId);
            AuthToken newToken = new AuthToken();
            newToken.setUser(user);
            newToken.setDeviceInfo(idDevice);
            newToken.setRefreshToken(token);
            newToken.setRevoked(false);
            newToken.setExpiresAt(Instant.now().plusSeconds(refreshTokenExpiration));
            newToken.setFcmToken(fcmToken);

            authTokenRepository.save(newToken);
        }
    }

    public AuthToken getUserTokensByRefreshTokenAndIdAccountAndIdDevice(
            String refreshToken, String idUser, String idDevice) {
        UUID userId;
        try {
            userId = UUID.fromString(idUser);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("User ID không đúng định dạng UUID");
        }

        return authTokenRepository.findByUser_UserIdAndDeviceInfo(userId, idDevice)
                .filter(token -> refreshToken.equals(token.getRefreshToken()))
                .orElse(null);
    }

    public AuthToken getUserTokenByRefreshToken(String refreshToken) {
        return authTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Phiên đăng nhập không tồn tại hoặc Token sai"));
    }

    public void updateFcmTokenOnly(String refreshToken, String fcmToken) {
        AuthToken authToken = authTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Phiên đăng nhập không tồn tại hoặc Token sai"));
        // 2. Cập nhật FCM Token mới
        authToken.setFcmToken(fcmToken);

        // 3. Lưu lại
        authTokenRepository.save(authToken);
    }

    public List<String> getAllFcmTokensByUserId(UUID userId) {
        List<AuthToken> tokens = authTokenRepository.findAllByUser_UserIdAndRevokedFalse(userId);
        return tokens.stream()
                .map(AuthToken::getFcmToken)
                .filter(fcmToken -> fcmToken != null && !fcmToken.isEmpty())
                .collect(Collectors.toList());
    }
}
