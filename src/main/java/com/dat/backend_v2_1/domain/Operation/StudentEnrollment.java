package com.dat.backend_v2_1.domain.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.enums.Operation.StudentEnrollmentStatus;
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
        name = "student_enrollment",
        schema = "operation",
        // Index giúp tìm kiếm nhanh: Tìm lịch sử lớp học của 1 học sinh, hoặc tìm danh sách học viên của 1 lớp học
        indexes = {
                @Index(name = "idx_enrollment_student", columnList = "student_user_id"),
                @Index(name = "idx_enrollment_schedule", columnList = "schedule_id"),
                @Index(name = "idx_enrollment_status", columnList = "enrollment_status") // Hay lọc theo trạng thái
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentEnrollment {
    @Id
    @GeneratedValue(generator = "uuid-hibernate-generator")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "enrollment_id", updatable = false, nullable = false)
    UUID enrollmentId; // Chuyển sang UUID cho đồng bộ hệ thống

    // --- THÔNG TIN LIÊN KẾT ---

    @NotNull(message = "Học viên không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    Student student;

    @NotNull(message = "Lớp học không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    ClassSchedule classSchedule;

    // --- THÔNG TIN THỜI GIAN & TRẠNG THÁI ---

    @NotNull(message = "Ngày nhập học không được để trống")
    @Column(name = "join_date", nullable = false)
    LocalDate joinDate;

    @Column(name = "leave_date")
    LocalDate leaveDate; // Có thể null nếu đang học

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "enrollment_status", nullable = false, length = 20)
    StudentEnrollmentStatus status = StudentEnrollmentStatus.ACTIVE; // Mặc định là Đang học

    // --- GHI CHÚ ---

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    @Column(name = "note", length = 500)
    String note; // Lý do chuyển lớp, bảo lưu, v.v.

    // --- AUDIT ---

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    Instant updatedAt;
}
