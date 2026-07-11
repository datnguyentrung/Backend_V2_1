package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.CoachTimesheet;
import com.dat.backend_v2_1.dto.Operation.CoachTimesheetDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface CoachTimesheetRepositoryCustom {
    Page<CoachTimesheet> findAllWithEntityGraph(Specification<CoachTimesheet> spec, Pageable pageable);

    CoachTimesheetDTO.SummaryResponse getSummary(Specification<CoachTimesheet> spec);
}
