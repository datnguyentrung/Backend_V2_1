package com.dat.backend_v2_1.service.Security;

import com.dat.backend_v2_1.domain.Security.AuthToken;
import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.repository.Security.AuthTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthTokenService {
    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private UserService userService;

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

    public void updateUserTokens(String token, String idUser, String idDevice) {
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
            authTokenRepository.save(currentUser);
        } else {
            // Create new token with refreshToken
            User user = userService.getUserById(userId);
            AuthToken newToken = new AuthToken();
            newToken.setUser(user);
            newToken.setDeviceInfo(idDevice);
            newToken.setRefreshToken(token);
            newToken.setExpiresAt(Instant.now().plusSeconds(refreshTokenExpiration));
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
}
