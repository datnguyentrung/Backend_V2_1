package com.dat.backend_v2_1.domain.Security;

import com.dat.backend_v2_1.enums.Security.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "User", schema = "security")
public class User {
    @Id
    @GeneratedValue(generator = "uuid-hibernate-generator")
    @UuidGenerator // Hibernate 6+ (Tự động sinh UUID chuẩn)
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false) // Tên cột foreign key chuẩn: user_id
    @ToString.Exclude // Ngắt vòng lặp vô hạn khi in log
    private Role role;
}
