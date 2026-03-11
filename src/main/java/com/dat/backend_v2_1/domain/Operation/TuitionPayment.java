package com.dat.backend_v2_1.domain.Operation;

import com.dat.backend_v2_1.domain.Core.Student;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "tuition_payment",
        schema = "operation"
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TuitionPayment {
    @Id
    @GeneratedValue(generator = "uuid-hibernate-generator")
    @UuidGenerator
    @Column(name = "payment_id")
    UUID paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    Student student;

    @Column(name = "total_amount", nullable = false)
    BigDecimal totalAmount; // Tổng số tiền thực tế nhận được (ví dụ: 1.350.000)

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    Instant updatedAt;

    @Column(name = "note")
    String note; // Ví dụ: "Mẹ đóng học phí quý 3"
}
