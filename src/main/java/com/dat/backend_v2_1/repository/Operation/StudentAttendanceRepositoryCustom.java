package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.StudentAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Custom Repository Interface để xử lý các query phức tạp
 * và apply EntityGraph với Specification.
 */
public interface StudentAttendanceRepositoryCustom {

    /**
     * Query với Specification + Pageable + Named EntityGraph để tránh N+1 query.
     *
     * @param spec     Specification chứa các điều kiện filter
     * @param pageable Thông tin phân trang và sắp xếp
     * @return Page chứa danh sách StudentAttendance với eager loaded relationships
     */
    Page<StudentAttendance> findAllWithEntityGraph(Specification<StudentAttendance> spec, Pageable pageable);
}

