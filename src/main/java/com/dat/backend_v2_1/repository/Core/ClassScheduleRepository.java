package com.dat.backend_v2_1.repository.Core;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, String> {
}
