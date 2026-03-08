package com.dat.backend_v2_1.domain.Operation;

import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.enums.Operation.AttendanceStatus;
import com.dat.backend_v2_1.enums.Operation.EvaluationStatus;
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
@NamedEntityGraph(
        name = "StudentAttendance.withDetails",
        attributeNodes = {
                @NamedAttributeNode(value = "studentEnrollment", subgraph = "enrollment-subgraph"),
                @NamedAttributeNode("recordedByCoach"),
                @NamedAttributeNode("evaluatedByCoach")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "enrollment-subgraph",
                        attributeNodes = {
                                @NamedAttributeNode("student"),
                                @NamedAttributeNode(value = "classSchedule", subgraph = "schedule-subgraph")
                        }
                ),
                @NamedSubgraph(
                        name = "schedule-subgraph",
                        attributeNodes = {
                                @NamedAttributeNode("branch")
                        }
                )
        }
)
@Table(
        name = "student_attendance",
        schema = "operation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_enrollment_date",
                        columnNames = {"student_enrollment_id", "session_date"}
                )
        },
        // Index giúp tìm kiếm nhanh: Tìm lịch sử đi học của 1 học sinh, hoặc tìm danh sách điểm danh của 1 buổi học
        indexes = {
                @Index(name = "idx_student_enrollment", columnList = "student_enrollment_id"),
                @Index(name = "idx_session_date", columnList = "session_date DESC")
//                @Index(name = "idx_attendance_schedule_date", columnList = "schedule_id, session_date")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentAttendance {

    @Id
    @GeneratedValue(generator = "uuid-hibernate-generator")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "attendance_id", updatable = false, nullable = false)
    UUID attendanceId; // Nên thống nhất đặt là id cho gọn, hoặc attendanceId tùy convention team

    // --- THÔNG TIN CƠ BẢN ---

    @ManyToOne
    @JoinColumn(name = "student_enrollment_id", nullable = true)
    StudentEnrollment studentEnrollment;

    @NotNull(message = "Ngày học không được để trống")
    @Column(name = "session_date", nullable = false)
    LocalDate sessionDate; // Đổi tên: attendanceDate -> sessionDate (Ngày diễn ra buổi học theo lịch)

    // --- TRẠNG THÁI ĐIỂM DANH ---

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "attendance_status", nullable = false, length = 20)
    AttendanceStatus attendanceStatus = AttendanceStatus.ABSENT; // Mặc định là vắng

    @Column(name = "check_in_time")
    Instant checkInTime; // Đổi tên: attendanceTime -> checkInTime (Thời điểm thực tế quẹt thẻ/điểm danh)

    // Người thực hiện điểm danh (Có thể là Lễ tân hoặc HLV)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_coach_id") // Nếu cho phép NULL (ví dụ hệ thống tự điểm danh) thì bỏ @NotNull
            Coach recordedByCoach;

    // --- ĐÁNH GIÁ (EVALUATION) ---

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", length = 20)
    EvaluationStatus evaluationStatus; // Ví dụ: PASSED, FAILED, GOOD, EXCELLENT

    // HLV đánh giá (Có thể khác người điểm danh)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_coach_id")
    Coach evaluatedByCoach;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    @Column(name = "note", length = 500)
    String note;

    // --- AUDIT ---

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    Instant updatedAt;
}