package com.dat.ai_receptionist_web.repository.Operation;

import com.dat.ai_receptionist_web.domain.Operation.StudentAttendance;
import com.dat.ai_receptionist_web.dto.Operation.StudentAttendanceDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Map;

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

    StudentAttendanceDTO.AttendanceStats getStatistics(Specification<StudentAttendance> spec);

    Map<String, StudentAttendanceDTO.AttendanceStats> getStatisticsGroupedByStudent(LocalDate startDate, LocalDate endDate);
}

