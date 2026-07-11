package com.dat.backend_v2_1.domain.Operation;

import com.dat.backend_v2_1.enums.Operation.CoachTimesheetStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
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
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@NamedEntityGraph(
        name = "CoachTimesheet.withDetails",
        attributeNodes = {
                @NamedAttributeNode(value = "coachAssignment", subgraph = "assignment-subgraph")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "assignment-subgraph",
                        attributeNodes = {
                                @NamedAttributeNode("coach"),
                                @NamedAttributeNode(value = "classSchedule", subgraph = "schedule-subgraph")
                        }
                ),
                @NamedSubgraph(
                        name = "schedule-subgraph",
                        attributeNodes = @NamedAttributeNode("branch")
                )
        }
)
@Table(
        name = "coach_timesheet",
        schema = "operation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coach_schedule_date",
                        columnNames = {"coach_assignemnt", "working_date"}
                )
        },
        indexes = {
                @Index(name = "idx_ct_working_date", columnList = "working_date DESC"),
                @Index(name = "idx_ct_status", columnList = "status")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoachTimesheet {

    @Id
    @GeneratedValue(generator = "uuid-hibernate-generator")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "timesheet_id", updatable = false, nullable = false)
    UUID timesheetId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_assignemnt", nullable = false)
    CoachAssignment coachAssignment;

    @NotNull(message = "Ngày làm việc không được để trống")
    @PastOrPresent(message = "Ngày làm việc không hợp lệ")
    @Column(name = "working_date", nullable = false)
    LocalDate workingDate;

    @Column(name = "check_in_time")
    LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    LocalDateTime checkOutTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    CoachTimesheetStatus status = CoachTimesheetStatus.CHECKED_IN;

    @Size(max = 500)
    @Column(name = "note", length = 500)
    String note;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
