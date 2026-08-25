package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import com.dat.ai_receptionist_web.dto.Catalog.ClassScheduleDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.mapper.Catalog.ClassScheduleMapper;
import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Core.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassScheduleService {
    private final ClassScheduleRepository classScheduleRepository;
    private final ClassScheduleMapper classScheduleMapper;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public PageResponse<ClassScheduleDTO.Response> list(Pageable pageable) {
        return PageResponse.of(classScheduleRepository.findAll(pageable), classScheduleMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ClassScheduleDTO.Response get(UUID id) {
        return classScheduleMapper.toResponse(find(id));
    }

    @Transactional
    public ClassScheduleDTO.Response create(ClassScheduleDTO.CreateRequest request) {
        ClassSchedule classSchedule = new ClassSchedule();
        classSchedule.setBranch(branchRepository.findById(request.branchId())
                .orElseThrow(() -> new IllegalArgumentException("Branch not found")));
        classSchedule.setWeekday(request.weekday());
        classSchedule.setLevel(request.level());
        classSchedule.setLocation(request.location());
        classSchedule.setStatus(request.status());
        classSchedule.setStartTime(request.startTime());
        classSchedule.setEndTime(request.endTime());
        return classScheduleMapper.toResponse(classScheduleRepository.save(classSchedule));
    }

    @Transactional
    public ClassScheduleDTO.Response update(UUID id, ClassScheduleDTO.UpdateRequest request) {
        ClassSchedule classSchedule = find(id);
        classSchedule.setBranch(branchRepository.findById(request.branchId())
                .orElseThrow(() -> new IllegalArgumentException("Branch not found")));
        classScheduleMapper.updateEntity(request, classSchedule);
        return classScheduleMapper.toResponse(classScheduleRepository.save(classSchedule));
    }

    @Transactional
    public void delete(UUID id) {
        ClassSchedule classSchedule = find(id);
        classSchedule.setStatus(ScheduleStatus.INACTIVE);
    }

    private ClassSchedule find(UUID id) {
        return classScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ClassSchedule not found"));
    }
}
