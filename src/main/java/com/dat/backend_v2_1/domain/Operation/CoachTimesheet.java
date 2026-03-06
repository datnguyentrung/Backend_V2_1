package com.dat.backend_v2_1.domain.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Core.Coach;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "coach_timesheet",
        schema = "operation",
        // --- QUAN TRỌNG: Ràng buộc duy nhất (Unique Constraint) ---
        // Đặt tên constraint (name) để dễ debug khi có lỗi Duplicate entry
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coach_schedule_date",
                        columnNames = {"coach_user_id", "schedule_id", "working_date"}
                )
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoachTimesheet {

    @Id
    @GeneratedValue(generator = "uuid-hibernate-generator")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "timesheet_id", updatable = false, nullable = false)
    UUID timesheetId; // Đổi tên timesheetId -> id cho ngắn gọn, chuẩn JPA

    // --- RELATIONSHIPS ---

    @NotNull(message = "HLV không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_user_id", nullable = false) // Tên cột khớp với uniqueConstraints bên trên
    Coach coach;

    @NotNull(message = "Lịch dạy không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false) // Rút gọn tên cột cho sạch đẹp
    ClassSchedule classSchedule;

    // --- BUSINESS DATA ---

    @NotNull(message = "Ngày làm việc không được để trống")
    @PastOrPresent(message = "Ngày làm việc không hợp lệ")
    @Column(name = "working_date", nullable = false)
    LocalDate workingDate;

    @Column(name = "check_in_time")
    Instant checkInTime; // Thời gian thực tế HLV bấm nút "Check-in"

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    CoachTimesheetStatus status = CoachTimesheetStatus.PENDING; // Mặc định là Chờ duyệt

    @Size(max = 500)
    @Column(name = "note", length = 500)
    String note;

    // --- AUDIT FIELDS ---
    // Rất quan trọng với dữ liệu lương thưởng (chứng minh ai tạo/sửa bảng công)

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    Instant updatedAt;
}
