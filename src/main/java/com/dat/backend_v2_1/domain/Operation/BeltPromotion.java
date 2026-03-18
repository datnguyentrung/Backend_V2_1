package com.dat.backend_v2_1.domain.Operation;

import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.enums.Core.Belt;
import com.dat.backend_v2_1.enums.Operation.BeltPromotionResult;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
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
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "belt_promotion",
        schema = "operation",
        // Index giúp tìm nhanh lịch sử lên đai của 1 học viên
        indexes = {
                @Index(name = "idx_promotion_student", columnList = "student_id"),
                @Index(name = "idx_promotion_exam_date", columnList = "exam_date")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BeltPromotion {

    @Id
    @GeneratedValue(generator = "uuid-hibernate-generator")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "promotion_id", updatable = false, nullable = false)
    UUID promotionId;

    // --- NGƯỜI THAM GIA ---

    @NotNull(message = "Học viên không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    // Nên dùng Student thay vì User chung chung để type-safe hơn
    Student student;

    // --- THÔNG TIN KỲ THI ---

    @NotNull(message = "Ngày thi không được để trống")
    @PastOrPresent(message = "Ngày thi phải là hôm nay hoặc trong quá khứ")
    @Column(name = "exam_date", nullable = false)
    LocalDate examDate;

    @NotNull(message = "Đai hiện tại không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "current_belt", nullable = false, length = 20)
    Belt currentBelt;

    @NotNull(message = "Đai mục tiêu không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "target_belt", nullable = false, length = 20)
    Belt targetBelt;

    @NotNull(message = "Kết quả không được để trống")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "result", nullable = false, length = 20)
    BeltPromotionResult result = BeltPromotionResult.PENDING; // Mặc định là Đang chờ duyệt

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    @Column(name = "note", length = 500)
    String note; // Nhận xét của HLV (VD: Kỹ thuật tốt, cần cải thiện thể lực)

    // --- AUDIT ---

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // --- VALIDATION LOGIC (Tùy chọn) ---

    @AssertTrue(message = "Đai mục tiêu phải khác đai hiện tại")
    private boolean isTargetBeltValid() {
        if (currentBelt == null || targetBelt == null) return true;
        return !currentBelt.equals(targetBelt);
    }
}