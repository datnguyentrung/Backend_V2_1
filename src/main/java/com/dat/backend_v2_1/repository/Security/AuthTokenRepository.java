package com.dat.backend_v2_1.repository.Security;

import com.dat.backend_v2_1.domain.Security.AuthToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    Optional<AuthToken> findBySessionId(String sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AuthToken t join fetch t.user where t.refreshTokenHash = :refreshTokenHash")
    Optional<AuthToken> findByRefreshTokenHashForUpdate(@Param("refreshTokenHash") String refreshTokenHash);

    Optional<AuthToken> findByRefreshTokenHash(String refreshTokenHash);

    List<AuthToken> findAllByUser_UserIdAndRevokedFalse(UUID userId);

    List<AuthToken> findAllByUser_UserIdInAndRevokedFalse(Collection<UUID> userIds);

    Optional<AuthToken> findByFcmToken(String fcmToken);

    List<AuthToken> findAllByUser_UserId(UUID userId);

    List<AuthToken> findAllByActivePerson_PersonIdAndRevokedFalse(UUID activePersonPersonId);

    @Query("""
            SELECT DISTINCT t.fcmToken
            FROM AuthToken t
            WHERE t.activePerson.personId = :personId
              AND t.revoked = false
              AND t.fcmToken IS NOT NULL
              AND t.fcmToken <> ''
            """)
    List<String> findActiveFcmTokens(@Param("personId") UUID personId);

    @Query("""
            SELECT DISTINCT t.fcmToken
            FROM AuthToken t
            WHERE t.user.userId IN :userIds
              AND t.revoked = false
              AND t.fcmToken IS NOT NULL
              AND t.fcmToken <> ''
            """)
    List<String> findActiveFcmTokensByUserIds(@Param("userIds") Collection<UUID> userIds);
}
