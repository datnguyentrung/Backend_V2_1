package com.dat.backend_v2_1.domain.Security;

import com.dat.backend_v2_1.enums.Security.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "user",
        schema = "security",
        indexes = {
                @Index(name = "idx_user_phone", columnList = "phone_number"),
                @Index(name = "idx_user_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_phone", columnNames = "phone_number")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false, updatable = false)
    UUID userId;

    @NotBlank(message = "Phone number must not be blank")
    @Size(max = 20)
    @Pattern(regexp = "^0(3[2-9]|5[689]|7[06-9]|8[1-689]|9[0-46-9])\\d{7}$",
            message = "Phone number must be a normalized Vietnamese mobile number")
    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    String phoneNumber;

    @NotBlank(message = "Password hash must not be blank")
    @Column(name = "password_hash", nullable = false, length = 255)
    String passwordHash;

    @NotNull(message = "User status must not be null")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            schema = "security",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_code", referencedColumnName = "role_code"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_user_role",
                    columnNames = {"user_id", "role_code"}
            )
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Set<Role> roles = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    LocalDateTime lastLoginAt;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    List<UserProfile> profiles = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    List<AuthToken> authTokens = new ArrayList<>();
}
