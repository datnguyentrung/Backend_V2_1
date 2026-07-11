package com.dat.backend_v2_1.domain.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
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
@NamedEntityGraph(
        name = "CoachAssignment.withDetails",
        attributeNodes = {
                @NamedAttributeNode("coach"),
                @NamedAttributeNode(value = "classSchedule", subgraph = "schedule-subgraph")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "schedule-subgraph",
                        attributeNodes = @NamedAttributeNode("branch")
                )
        }
)
@Table(
        name = "coach_assignment",
        schema = "operation",
        indexes = {
                @Index(name = "idx_assignment_coach", columnList = "coach_user_id"),
                @Index(name = "idx_assignment_schedule", columnList = "schedule_id"),
                @Index(name = "idx_assignment_status_dates", columnList = "assignment_status, assigned_date, end_date")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoachAssignment {

    @Id
    @GeneratedValue(generator = "uuid-hibernate-generator")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "assignment_id", updatable = false, nullable = false)
    UUID assignmentId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_user_id", nullable = false)
    Coach coach;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    ClassSchedule classSchedule;

    @NotNull
    @Column(name = "assigned_date", nullable = false)
    LocalDate assignedDate;

    @Column(name = "end_date")
    LocalDate endDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "assignment_status", nullable = false, length = 20)
    CoachAssignmentStatus status = CoachAssignmentStatus.ACTIVE;

    @Size(max = 500)
    @Column(name = "note", length = 500)
    String note;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    public boolean isEffectiveOn(LocalDate date) {
        if (date == null || status != CoachAssignmentStatus.ACTIVE) {
            return false;
        }
        boolean started = assignedDate == null || !date.isBefore(assignedDate);
        boolean notEnded = endDate == null || !date.isAfter(endDate);
        return started && notEnded;
    }
}
