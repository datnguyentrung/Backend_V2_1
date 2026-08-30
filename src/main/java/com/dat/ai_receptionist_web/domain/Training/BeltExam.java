package com.dat.ai_receptionist_web.domain.Training;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Core.Belt;

import com.dat.ai_receptionist_web.enums.Training.BeltExamResult;
import com.dat.ai_receptionist_web.enums.Training.BeltExamType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Check;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "belt_exam",
        schema = "training",
        indexes = {
                @Index(name = "idx_belt_exam_person_period", columnList = "person_id,year,quarter"),
                @Index(name = "idx_belt_exam_type_result", columnList = "type,result"),
                @Index(name = "idx_belt_exam_exam_date", columnList = "exam_date"),
                @Index(name = "idx_belt_exam_created_by", columnList = "created_by_user_id")
        }
)
@Check(
        name = "chk_belt_exam_business_rule",
        constraints = """
                year > 0
                AND quarter BETWEEN 1 AND 4
                AND from_belt <> target_belt
                AND (
                    result = 'PENDING'
                    OR exam_date IS NOT NULL
                )
                """
)
public class BeltExam {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "belt_exam_id", nullable = false, updatable = false)
    private UUID beltExamId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "person_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_belt_exam_person")
    )
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_belt", nullable = false, length = 20)
    private Belt fromBelt;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_belt", nullable = false, length = 20)
    private Belt targetBelt;

    @Min(1)
    @Column(name = "year", nullable = false)
    private Integer year;

    @Min(1)
    @Max(4)
    @Column(name = "quarter", nullable = false)
    private Integer quarter;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private BeltExamResult result = BeltExamResult.PENDING;

    @Size(max = 1000)
    @Column(name = "note", length = 1000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_belt_exam_created_by")
    )
    private User createdByUser;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private BeltExamType type;

    @PrePersist
    @PreUpdate
    private void validateInvariant() {
        if (person == null) {
            throw new IllegalStateException("person must not be null");
        }
        if (fromBelt == null) {
            throw new IllegalStateException("fromBelt must not be null");
        }
        if (targetBelt == null) {
            throw new IllegalStateException("targetBelt must not be null");
        }
        if (fromBelt == targetBelt) {
            throw new IllegalStateException(
                    "fromBelt and targetBelt must be different"
            );
        }
        if (year == null || year <= 0) {
            throw new IllegalStateException("year must be greater than 0");
        }
        if (quarter == null || quarter < 1 || quarter > 4) {
            throw new IllegalStateException("quarter must be between 1 and 4");
        }
        if (result == null) {
            throw new IllegalStateException("result must not be null");
        }
        if (type == null) {
            throw new IllegalStateException("type must not be null");
        }
        if (result != BeltExamResult.PENDING && examDate == null) {
            throw new IllegalStateException(
                    "examDate is required when result is not PENDING"
            );
        }
        if (createdByUser == null) {
            throw new IllegalStateException("createdByUser must not be null");
        }
    }
}
