package com.dat.ai_receptionist_web.domain.Operation;

import com.dat.ai_receptionist_web.domain.Core.ClassSchedule;
import com.dat.ai_receptionist_web.enums.Operation.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "class_session",
        schema = "operation",
        indexes = {
                // 1. Tối ưu cho các Cron Job quét tự động chuyển trạng thái
                // Cover các hàm: activateScheduledSessions, completeScheduledSessions, findClassSessionToClose
                @Index(name = "idx_cs_date_status", columnList = "session_date DESC, status"),

                // 2. Tối ưu cho khóa ngoại và query lọc danh sách buổi học theo lịch
                // Cover hàm: findBySessionDateAndClassSchedule_ScheduleIdIn và các thao tác JOIN với ClassSchedule
                @Index(name = "idx_cs_schedule_date", columnList = "class_schedule_schedule_id, session_date DESC")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassSession {
    @Id
    @GeneratedValue(generator = "uuid-hibernate-generator")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "session_id", updatable = false, nullable = false)
    UUID sessionId;

    @Column(name = "session_date", nullable = false)
    @Builder.Default
    LocalDate sessionDate = LocalDate.now(); // Mặc định là ngày hiện tại, có thể được cập nhật khi tạo mới hoặc chỉnh sửa buổi học

    @ManyToOne(fetch = FetchType.LAZY) // Nên thêm Lazy fetch để tối ưu hiệu suất
    @JoinColumn(name = "class_schedule_schedule_id")
    ClassSchedule classSchedule;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    SessionStatus status = SessionStatus.SCHEDULED; // Ví dụ: SCHEDULED, COMPLETED, CANCELED

    @Column(name = "is_attendance_closed", nullable = false)
    @Builder.Default
    boolean isAttendanceClosed = false; // Mặc định là false, khi giáo viên đóng điểm danh sẽ chuyển thành true

    @Column(name = "start_time", nullable = false)
    LocalTime startTime; // Bỏ @Builder.Default và phép gán trống

    @Column(name = "end_time")
    LocalTime endTime;

    @Column(name = "note", length = 500)
    String note;

    // --- Lifecycle Callbacks ---
    @PrePersist
    private void prePersist() {
        // Đảm bảo sessionDate có giá trị
        if (this.sessionDate == null) {
            this.sessionDate = LocalDate.now();
        }

        // Chỉ cần lấy thẳng LocalTime từ ClassSchedule gán sang
        if (this.classSchedule != null) {
            if (this.startTime == null && this.classSchedule.getStartTime() != null) {
                this.startTime = this.classSchedule.getStartTime();
            }
            if (this.endTime == null && this.classSchedule.getEndTime() != null) {
                this.endTime = this.classSchedule.getEndTime();
            }
        }
    }

    @Override
    public String toString() {
        return "ClassSession{" +
                "sessionId=" + sessionId +
                ", sessionDate=" + sessionDate +
                ", classSchedule=" + (classSchedule != null ? classSchedule.getScheduleId() : null) +
                ", status=" + status +
                ", isAttendanceClosed=" + isAttendanceClosed +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", note='" + note + '\'' +
                '}';
    }
}
