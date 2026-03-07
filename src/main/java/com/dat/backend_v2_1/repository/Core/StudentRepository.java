package com.dat.backend_v2_1.repository.Core;

import com.dat.backend_v2_1.domain.Core.Student;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    boolean existsByPhoneNumber(@NotBlank(message = "Số điện thoại không được để trống") @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
            message = "Số điện thoại không đúng định dạng Việt Nam") String phoneNumber);

    boolean existsByNationalCode(String nationalCode);

    boolean existsByStudentCode(String generatedCode);

    @Query("""
                    SELECT DISTINCT s FROM Student s
                    LEFT JOIN FETCH s.branch
                    LEFT JOIN FETCH s.role
                    WHERE (LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(s.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))
                    AND (:status IS NULL OR s.studentStatus = :status)
            """)
    Page<Student> findStudentsWithFilter(
            @Param("search") String search,
            @Param("status") StudentStatus status,
            Pageable pageable
    );

    /**
     * Tối ưu: Lấy số lượng học viên theo tất cả trạng thái trong 1 query duy nhất
     * Thay vì gọi countByStudentStatus 3 lần riêng biệt (3 queries)
     *
     * @return List of Object[] where arr[0] is StudentStatus and arr[1] is count
     */
    @Query("""
                SELECT s.studentStatus, COUNT(s)
                FROM Student s
                GROUP BY s.studentStatus
            """)
    List<Object[]> countStudentsByStatusGrouped();
}
