package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.TuitionPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
    List<TuitionPayment> findByStudent_PersonIdOrderByCreatedAtDesc(UUID studentId);

    /**
     * Truy vấn lịch sử đóng phí (Payment + Detail) theo studentId
     */
    @Query("""
            SELECT tp FROM TuitionPayment tp
            JOIN FETCH tp.student s
            WHERE s.personId = :studentId
            ORDER BY tp.createdAt DESC
            """)
    List<TuitionPayment> findPaymentsWithStudentByStudentId(@Param("studentId") UUID studentId);

    /**
     * Tìm kiếm lịch sử đóng phí theo studentId và từ khóa search (tên lớp, mã lớp, ghi chú)
     * Sắp xếp mới nhất trước
     */
    @EntityGraph(attributePaths = {"student"})
    @Query("SELECT p FROM TuitionPayment p " +
            "WHERE (:search IS NULL OR :search = '') " +
            "   OR LOWER(p.student.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "   OR LOWER(p.student.studentCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "   OR LOWER(p.note) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<TuitionPayment> findTuitionPaymentHistory(
            @Param("search") String search,
            Pageable pageable
    );
}
