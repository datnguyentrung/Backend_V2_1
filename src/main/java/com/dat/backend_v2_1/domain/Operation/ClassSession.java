package com.dat.backend_v2_1.domain.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.enums.Operation.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "class-session", schema = "operation") // Tên bảng snake_case
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassSession {
    @Id
    @GeneratedValue(generator = "uuid-hibernate-generator")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "session_id", updatable = false, nullable = false)
    UUID sessionId;

    @Column(name = "session_date", nullable = false)
    LocalDate sessionDate = LocalDate.now(); // Mặc định là ngày hiện tại, có thể được cập nhật khi tạo mới hoặc chỉnh sửa buổi học

    @ManyToOne
    @JoinColumn(name = "class_schedule_schedule_id")
    ClassSchedule classSchedule; // Liên kết với ClassSchedule để biết ngày, giờ, trình độ, chi nhánh của buổi học

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    SessionStatus status = SessionStatus.ACTIVE; // Ví dụ: SCHEDULED, COMPLETED, CANCELED

    @Column(name = "is_attendance_closed", nullable = false)
    @Builder.Default
    boolean isAttendanceClosed = false; // Mặc định là false, khi giáo viên đóng điểm danh sẽ chuyển thành true

    @Column(name = "note", length = 500)
    String note;
}
