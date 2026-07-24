package com.dat.ai_receptionist_web.repository.Operation;

import com.dat.ai_receptionist_web.domain.Operation.CoachTimesheet;
import com.dat.ai_receptionist_web.dto.Operation.CoachTimesheetDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface CoachTimesheetRepositoryCustom {
    Page<CoachTimesheet> findAllWithEntityGraph(Specification<CoachTimesheet> spec, Pageable pageable);

    CoachTimesheetDTO.SummaryResponse getSummary(Specification<CoachTimesheet> spec);
}
