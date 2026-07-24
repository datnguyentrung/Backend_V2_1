package com.dat.ai_receptionist_web.domain.Security;

import com.dat.ai_receptionist_web.domain.Core.Person;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "auth_tokens",
        schema = "security",
        indexes = {
                @Index(name = "idx_auth_token_session", columnList = "session_id"),
                @Index(name = "idx_auth_token_refresh_hash", columnList = "refresh_token_hash"),
                @Index(name = "idx_auth_token_user", columnList = "user_id")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthToken {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "token_id", updatable = false, nullable = false)
    UUID tokenId;

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    String sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_auth_token_user"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    User user;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    String refreshTokenHash;

    @Column(name = "device_info", length = 255)
    String deviceInfo;

    @Column(name = "fcm_token", length = 500)
    String fcmToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_person_id", foreignKey = @ForeignKey(name = "fk_auth_token_active_person"))
    @ToString.Exclude
    Person activePerson;

    @Column(name = "active_context_type", length = 30)
    String activeContextType;

    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "revoked", nullable = false)
    boolean revoked = false;

    @Column(name = "revoked_at")
    LocalDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "last_used_at")
    LocalDateTime lastUsedAt;

    @Version
    @Column(name = "version", nullable = false)
    long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.lastUsedAt = now;
    }
}
