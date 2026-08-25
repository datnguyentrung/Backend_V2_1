package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.CoachTimesheet;
import com.dat.ai_receptionist_web.dto.Training.CoachTimesheetDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Training.CoachTimesheetMapper;
import com.dat.ai_receptionist_web.repository.Training.CoachTimesheetRepository;
import com.dat.ai_receptionist_web.repository.Training.CoachAssignmentRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoachTimesheetCrudService {
    private final CoachTimesheetRepository repository;
    private final CoachTimesheetMapper mapper;
    private final CoachAssignmentRepository coachAssignmentRepository;
    private final ClassSessionRepository classSessionRepository;

    @Transactional(readOnly = true)
    public PageResponse<CoachTimesheetDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CoachTimesheetDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public CoachTimesheetDTO.Response create(CoachTimesheetDTO.CreateRequest request) {
        CoachTimesheet entity = new CoachTimesheet();
        entity.setCoachAssignment(coachAssignmentRepository.findById(request.coachAssignmentId()).orElseThrow(() -> new IllegalArgumentException("CoachAssignment not found")));
        entity.setClassSession(classSessionRepository.findById(request.classSessionId()).orElseThrow(() -> new IllegalArgumentException("ClassSession not found")));
        entity.setCheckInTime(request.checkInTime());
        entity.setCheckOutTime(request.checkOutTime());
        entity.setNote(request.note());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public CoachTimesheetDTO.Response update(UUID id, CoachTimesheetDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setCoachAssignment(coachAssignmentRepository.findById(request.coachAssignmentId()).orElseThrow(() -> new IllegalArgumentException("CoachAssignment not found")));
        entity.setClassSession(classSessionRepository.findById(request.classSessionId()).orElseThrow(() -> new IllegalArgumentException("ClassSession not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        repository.delete(entity);
    }

    private CoachTimesheet find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("CoachTimesheet not found"));
    }
}
