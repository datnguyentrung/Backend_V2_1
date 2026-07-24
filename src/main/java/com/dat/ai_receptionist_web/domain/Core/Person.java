package com.dat.ai_receptionist_web.domain.Core;

import com.dat.ai_receptionist_web.enums.Core.Belt;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Inheritance(strategy = InheritanceType.JOINED)
@Table(
        name = "person",
        schema = "core",
        indexes = {
                @Index(name = "idx_person_full_name", columnList = "full_name"),
                @Index(name = "idx_person_national_code", columnList = "national_code")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_person_national_code", columnNames = "national_code")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Person {


    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "person_id", nullable = false, updatable = false)
    UUID personId;

    @NotBlank(message = "Full name must not be blank")
    @Size(max = 100)
    @Column(name = "full_name", nullable = false, length = 100)
    String fullName;

    @Column(name = "gender")
    Boolean gender;

    @Past(message = "Birth date must be in the past")
    @Column(name = "birth_date")
    LocalDate birthDate;

    @Email(message = "Email is invalid")
    @Size(max = 100)
    @Column(name = "email", length = 100)
    String email;

    @Size(max = 50)
    @Column(name = "national_code", unique = true, length = 50)
    String nationalCode;

    @NotNull(message = "Belt must not be null")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "belt", nullable = false, length = 20)
    Belt belt = Belt.C10;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 512)
    @Column(name = "face_embedding", columnDefinition = "vector(512)")
    float[] faceEmbedding;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
