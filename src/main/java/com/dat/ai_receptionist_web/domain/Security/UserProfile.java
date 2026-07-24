package com.dat.ai_receptionist_web.domain.Security;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "user_profile",
        schema = "security",
        indexes = {
                @Index(
                        name = "idx_user_profile_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_user_profile_person",
                        columnList = "person_id"
                ),
                @Index(
                        name = "idx_user_profile_relationship",
                        columnList = "relationship_type"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_profile_relationship",
                        columnNames = {
                                "user_id",
                                "person_id",
                                "relationship_type"
                        }
                )
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfile {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(
            name = "user_profile_id",
            nullable = false,
            updatable = false
    )
    UUID userProfileId;

    /**
     * Tài khoản đăng nhập.
     */
    @NotNull(message = "Tài khoản không được để trống")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_profile_user")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    User user;

    /**
     * Hồ sơ mà tài khoản được phép truy cập.
     *
     * Person thực tế có thể là Student hoặc Coach.
     */
    @NotNull(message = "Hồ sơ người dùng không được để trống")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "person_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_profile_person")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    Person person;

    /**
     * Quan hệ giữa tài khoản và hồ sơ.
     *
     * OWNER:
     * Tài khoản của chính người đó.
     *
     * GUARDIAN:
     * Phụ huynh/người bảo hộ được quản lý học viên.
     *
     * MANAGER:
     * Quản lý được phép truy cập hồ sơ.
     */
    @NotNull(message = "Loại quan hệ không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(
            name = "relationship_type",
            nullable = false,
            length = 30
    )
    RelationshipType relationshipType;

    /**
     * Cho phép vô hiệu hóa quan hệ nhưng không cần xóa dữ liệu.
     */
    @Builder.Default
    @Column(
            name = "is_active",
            nullable = false
    )
    Boolean active = true;

    @CreatedDate
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    LocalDateTime createdAt;


}