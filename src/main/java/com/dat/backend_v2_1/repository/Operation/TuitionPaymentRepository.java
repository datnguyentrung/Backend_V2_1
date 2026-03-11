package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.TuitionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TuitionPaymentRepository extends JpaRepository<TuitionPayment, UUID> {

    /**
     * Lấy tất cả payment của 1 học viên, sắp xếp mới nhất trước
     */
    List<TuitionPayment> findByStudent_UserIdOrderByCreatedAtDesc(UUID studentId);

    /**
     * Truy vấn lịch sử đóng phí (Payment + Detail) theo studentId
     */
    @Query("""
            SELECT tp FROM TuitionPayment tp
            JOIN FETCH tp.student s
            WHERE s.userId = :studentId
            ORDER BY tp.createdAt DESC
            """)
    List<TuitionPayment> findPaymentsWithStudentByStudentId(@Param("studentId") UUID studentId);
}
