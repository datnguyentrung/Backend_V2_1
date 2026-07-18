package com.dat.backend_v2_1.domain.Core;

import com.dat.backend_v2_1.enums.Core.CoachStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "coach",
        schema = "core",
        indexes = {
                @Index(
                        name = "idx_coach_status",
                        columnList = "coach_status"
                ),
                @Index(
                        name = "idx_coach_staff_code",
                        columnList = "staff_code"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coach_staff_code",
                        columnNames = "staff_code"
                )
        }
)
@PrimaryKeyJoinColumn(
        name = "person_id",
        foreignKey = @ForeignKey(name = "fk_coach_person")
)
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OnDelete(action = OnDeleteAction.CASCADE)
public class Coach extends Person {

    @NotBlank(message = "Mã nhân viên không được để trống")
    @Size(max = 20, message = "Mã nhân viên tối đa 20 ký tự")
    @Column(
            name = "staff_code",
            nullable = false,
            unique = true,
            length = 20
    )
    String staffCode;

    @NotNull(message = "Trạng thái huấn luyện viên không được để trống")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "coach_status",
            nullable = false,
            length = 20
    )
    CoachStatus coachStatus = CoachStatus.ACTIVE;
}
