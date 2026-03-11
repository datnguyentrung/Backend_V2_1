package com.dat.backend_v2_1.domain.Operation;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "tuition_payment_detail", schema = "operation")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TuitionPaymentDetail {
    @Id
    @GeneratedValue(generator = "uuid-hibernate-generator")
    @UuidGenerator
    @Column(name = "detail_id")
    UUID detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    TuitionPayment tuitionPayment;

    // Quan trọng: Gắn với Enrollment để biết đóng cho lớp nào (P14C1 hay P17C1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    StudentEnrollment enrollment;

    @Column(name = "for_month", nullable = false)
    int forMonth; // Ví dụ: 8

    @Column(name = "for_year", nullable = false)
    int forYear; // Ví dụ: 2026

    @Column(name = "amount_allocated", nullable = false)
    BigDecimal amountAllocated; // Số tiền phân bổ cho tháng này (ví dụ: 450.000)
}