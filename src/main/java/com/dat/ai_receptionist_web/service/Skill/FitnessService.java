package com.dat.ai_receptionist_web.service.Skill;

import com.dat.ai_receptionist_web.domain.Skill.Fitness;
import com.dat.ai_receptionist_web.dto.Skill.FitnessDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Skill.FitnessMapper;
import com.dat.ai_receptionist_web.repository.Skill.FitnessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FitnessService {
    private final FitnessRepository repository;
    private final FitnessMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<FitnessDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public FitnessDTO.Response get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public FitnessDTO.Response create(FitnessDTO.CreateRequest request) {
        Fitness entity = new Fitness();
        entity.setScheduleLevel(request.scheduleLevel());
        entity.setAmount(request.amount());
        entity.setDuration(request.duration());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public FitnessDTO.Response update(Long id, FitnessDTO.UpdateRequest request) {
        var entity = find(id);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        var entity = find(id);
        repository.delete(entity);
    }

    private Fitness find(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Fitness not found"));
    }
}
