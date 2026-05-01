package com.dat.backend_v2_1.repository.Core;

import com.dat.backend_v2_1.domain.Core.Student;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface StudentRepositoryCustom {

    List<StudentStatusCount> countStudentsByStatus(Specification<Student> spec);

    interface StudentStatusCount {
        com.dat.backend_v2_1.enums.Core.StudentStatus getStatus();
        Long getCount();
    }
}
