package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.TuitionPaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TuitionPaymentDetailRepository extends JpaRepository<TuitionPaymentDetail, UUID> {

    /**
     * Kiểm tra học viên đã đóng phí cho 1 enrollment cụ thể trong tháng/năm chưa.
     * Dùng cho logic check-in.
     */
    Optional<TuitionPaymentDetail> findByEnrollment_EnrollmentIdAndForMonthAndForYear(
            UUID enrollmentId, int forMonth, int forYear);

    /**
     * Lấy tất cả detail theo enrollmentId — dùng cho lịch sử đóng phí 1 lớp.
     */
    @Query("""
            SELECT tpd FROM TuitionPaymentDetail tpd
            JOIN FETCH tpd.tuitionPayment tp
            WHERE tpd.enrollment.enrollmentId = :enrollmentId
            ORDER BY tpd.forYear DESC, tpd.forMonth DESC
            """)
    List<TuitionPaymentDetail> findByEnrollmentIdWithPayment(@Param("enrollmentId") UUID enrollmentId);

    /**
     * Lấy tất cả detail của 1 học viên (dùng cho tab lịch sử phụ huynh xem).
     */
    @Query("""
            SELECT tpd FROM TuitionPaymentDetail tpd
            JOIN FETCH tpd.tuitionPayment tp
            JOIN FETCH tpd.enrollment enr
            JOIN FETCH enr.classSchedule cs
            WHERE tp.student.userId = :studentId
            ORDER BY tpd.forYear DESC, tpd.forMonth DESC
            """)
    List<TuitionPaymentDetail> findAllByStudentIdWithDetails(@Param("studentId") UUID studentId);

    /**
     * Lấy tất cả detail trong 1 payment — tránh N+1 khi map response.
     */
    List<TuitionPaymentDetail> findByTuitionPayment_PaymentId(UUID paymentId);

    /**
     * Kiểm tra học viên đã đóng phí tháng hiện tại hay chưa (tất cả lớp đang ACTIVE).
     * Dùng cho lễ tân AI.
     */
    @Query("""
            SELECT tpd FROM TuitionPaymentDetail tpd
            JOIN FETCH tpd.enrollment enr
            JOIN FETCH enr.classSchedule cs
            WHERE enr.enrollmentId IN :enrollmentIds
              AND tpd.forMonth = :month
              AND tpd.forYear  = :year
            """)
    List<TuitionPaymentDetail> findPaidEnrollmentsForMonth(
            @Param("enrollmentIds") List<UUID> enrollmentIds,
            @Param("month") int month,
            @Param("year") int year);
}
