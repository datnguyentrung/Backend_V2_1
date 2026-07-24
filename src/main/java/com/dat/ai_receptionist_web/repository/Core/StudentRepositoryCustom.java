package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.Student;
import com.dat.ai_receptionist_web.enums.Core.StudentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface StudentRepositoryCustom {

    List<StudentStatusCount> countStudentsByStatus(Specification<Student> spec);

    interface StudentStatusCount {
        StudentStatus getStatus();
        Long getCount();
    }
}
