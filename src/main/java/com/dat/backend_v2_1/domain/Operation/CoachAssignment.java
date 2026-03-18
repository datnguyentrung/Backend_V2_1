package com.dat.backend_v2_1.domain.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.enums.Operation.CoachAssignmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Table(
        name = "coach_assignment", // Tên bảng: Phân công HLV
        schema = "operation",
        indexes = {
                @Index(name = "idx_assignment_coach", columnList = "coach_user_id"),
                @Index(name = "idx_assignment_schedule", columnList = "schedule_id")
        }
        // Ràng buộc: Một HLV không thể được phân công 2 lần vào cùng 1 lớp (tránh duplicate)
//        uniqueConstraints = {
//                @UniqueConstraint(columnNames = {"coach_user_id", "schedule_id"})
//        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoachAssignment {

    @Id
    @GeneratedValue(generator = "uuid-hibernate-generator")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "assignment_id", updatable = false, nullable = false)
    UUID assignmentId;

    // --- LIÊN KẾT ---

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_user_id", nullable = false)
    Coach coach;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    ClassSchedule classSchedule;

    // --- THỜI GIAN & TRẠNG THÁI ---

    @NotNull
    @Column(name = "assigned_date", nullable = false)
    LocalDate assignedDate; // Ngày bắt đầu nhận lớp

    @Column(name = "end_date")
    LocalDate endDate; // Ngày kết thúc nhiệm vụ (nếu bị đổi lớp)

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "assignment_status", nullable = false)
    CoachAssignmentStatus status = CoachAssignmentStatus.ACTIVE;

    @Column(name = "note")
    String note;

    // --- AUDIT ---
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}