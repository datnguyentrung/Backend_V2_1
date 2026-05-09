package com.dat.backend_v2_1.domain.Skill;

import com.dat.backend_v2_1.domain.Core.Coach;
import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.enums.Skill.SkillLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class) // BẮT BUỘC để @CreatedDate hoạt động
@Table(
        name = "fitness_record",
        schema = "skill",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_fitness_record", // Tên của constraint trong database
                        columnNames = {
                                "student_user_id",
                                "assessment_date",
                                "skill_level",
                                "duration",
                                "amount"
                        } // Danh sách tên CỘT (chữ thường có gạch dưới như trong db, không phải tên biến Java)
                )
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FitnessRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // Đổi tên cột để tránh đụng từ khóa SQL
    @Column(name = "assessment_date", nullable = false)
    LocalDate assessmentDate;

    @NotNull(message = "Học viên không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    Student student;

    @Positive(message = "Thời gian (duration) phải lớn hơn 0")
    @NotNull(message = "Thời gian không được để trống")
    @Column(name = "duration", nullable = false)
    Integer duration;

    @Positive(message = "Số lượng (amount) phải lớn hơn 0")
    @NotNull(message = "Số lượng không được để trống")
    @Column(name = "amount", nullable = false)
    Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", nullable = false)
    SkillLevel skillLevel; // Cấp độ kỹ năng (BASIC, ADVANCED, EXPERT)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_coach_id", nullable = false)
    Coach recordByCoach;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}