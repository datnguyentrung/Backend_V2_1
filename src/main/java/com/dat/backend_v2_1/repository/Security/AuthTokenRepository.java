package com.dat.backend_v2_1.repository.Security;

import com.dat.backend_v2_1.domain.Security.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    Optional<AuthToken> findByUser_UserIdAndDeviceInfo(UUID userUserId, String deviceInfo);

    Optional<AuthToken> findByRefreshToken(String refreshToken);

    List<AuthToken> findAllByUser_UserIdAndRevokedFalse(UUID userId);
}
