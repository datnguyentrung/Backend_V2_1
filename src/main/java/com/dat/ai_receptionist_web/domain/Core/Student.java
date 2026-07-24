package com.dat.ai_receptionist_web.domain.Core;

import com.dat.ai_receptionist_web.enums.Core.StudentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "student",
        schema = "core",
        indexes = {
                @Index(
                        name = "idx_student_status",
                        columnList = "student_status"
                ),
                @Index(
                        name = "idx_student_branch",
                        columnList = "branch_id"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_code",
                        columnNames = "student_code"
                )
        }
)
@PrimaryKeyJoinColumn(
        name = "person_id",
        foreignKey = @ForeignKey(name = "fk_student_person")
)
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OnDelete(action = OnDeleteAction.CASCADE)
public class Student extends Person {

    @NotBlank(message = "Mã học viên không được để trống")
    @Size(max = 50, message = "Mã học viên tối đa 50 ký tự")
    @Column(
            name = "student_code",
            nullable = false,
            unique = true,
            length = 50
    )
    String studentCode;

    @NotNull(message = "Ngày bắt đầu tập không được để trống")
    @PastOrPresent(message = "Ngày bắt đầu không được ở tương lai")
    @Builder.Default
    @Column(
            name = "start_date",
            nullable = false
    )
    LocalDate startDate = LocalDate.now();

    @NotNull(message = "Trạng thái học viên không được để trống")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "student_status",
            nullable = false,
            length = 20
    )
    StudentStatus studentStatus = StudentStatus.ACTIVE;

    @NotNull(message = "Cơ sở không được để trống")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "branch_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_student_branch")
    )
    @ToString.Exclude
    Branch branch;

}
