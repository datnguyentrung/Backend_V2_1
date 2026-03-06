package com.dat.backend_v2_1.domain.Security;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder // Giúp tạo object dễ dàng hơn: AuthToken.builder()...build()
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "auth_tokens", // Tên bảng số nhiều, snake_case
        schema = "security",
        indexes = {
                @Index(name = "idx_refresh_token", columnList = "refresh_token") // Index để tìm kiếm nhanh
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthToken {

    @Id
    @GeneratedValue(generator = "uuid-hibernate-generator")
    @UuidGenerator // Hibernate 6+ (Tự động sinh UUID chuẩn)
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "token_id", updatable = false, nullable = false)
    UUID tokenId; // Đặt là 'id' thay vì 'tokenId' cho đúng chuẩn JPA

    // Quan hệ ManyToOne nên để LAZY để tránh query thừa khi không cần info User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // Tên cột foreign key chuẩn: user_id
    @ToString.Exclude // Ngắt vòng lặp vô hạn khi in log
    User user;

    @Column(name = "refresh_token", nullable = false, unique = true, length = 1024)
    String refreshToken; // Token thường dài, nên set length lớn hoặc dùng @Lob nếu cần

    @Column(name = "device_info")
    String deviceInfo;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt; // Đã sửa lỗi chính tả (expriresAt -> expiresAt)

    @Column(name = "revoked", nullable = false)
    @Builder.Default // Khi dùng Builder, giá trị này mặc định là false
    boolean revoked = false; // Dùng boolean nguyên thủy (tránh null), mặc định là false

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "fcm_token", length = 500) // Token Firebase khá dài, nên để 500 cho chắc
    String fcmToken;

    // Tự động set thời gian tạo
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.revoked) { // Đảm bảo logic nếu không dùng Builder
            // logic này thừa nếu đã init = false, nhưng giữ để chắc chắn
        }
    }
}
