package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import com.dat.ai_receptionist_web.dto.Catalog.ClassScheduleDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Catalog.ClassScheduleMapper;
import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Core.BranchRepository;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassScheduleCrudService {
    private final ClassScheduleRepository repository;
    private final ClassScheduleMapper mapper;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public PageResponse<ClassScheduleDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ClassScheduleDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public ClassScheduleDTO.Response create(ClassScheduleDTO.CreateRequest request) {
        ClassSchedule entity = new ClassSchedule();
        entity.setBranch(branchRepository.findById(request.branchId()).orElseThrow(() -> new IllegalArgumentException("Branch not found")));
        entity.setWeekday(request.weekday());
        entity.setLevel(request.level());
        entity.setLocation(request.location());
        entity.setStatus(request.status());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public ClassScheduleDTO.Response update(UUID id, ClassScheduleDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setBranch(branchRepository.findById(request.branchId()).orElseThrow(() -> new IllegalArgumentException("Branch not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setStatus(ScheduleStatus.INACTIVE);
    }

    private ClassSchedule find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("ClassSchedule not found"));
    }
}
