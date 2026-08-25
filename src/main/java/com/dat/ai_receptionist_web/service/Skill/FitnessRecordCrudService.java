package com.dat.ai_receptionist_web.service.Skill;

import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Skill.FitnessRecordMapper;
import com.dat.ai_receptionist_web.repository.Skill.FitnessRecordRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Skill.FitnessRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FitnessRecordCrudService {
    private final FitnessRecordRepository repository;
    private final FitnessRecordMapper mapper;
    private final PersonRepository personRepository;
    private final FitnessRepository fitnessRepository;

    @Transactional(readOnly = true)
    public PageResponse<FitnessRecordDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public FitnessRecordDTO.Response get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public FitnessRecordDTO.Response create(FitnessRecordDTO.CreateRequest request) {
        FitnessRecord entity = new FitnessRecord();
        entity.setStudent(personRepository.findById(request.studentId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        entity.setFitness(fitnessRepository.findById(request.fitnessId()).orElseThrow(() -> new IllegalArgumentException("Fitness not found")));
        entity.setRecordedByCoach(personRepository.findById(request.recordedByCoachId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        entity.setRecordDate(request.recordDate());
        entity.setDuration(request.duration());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public FitnessRecordDTO.Response update(Long id, FitnessRecordDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setStudent(personRepository.findById(request.studentId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        entity.setFitness(fitnessRepository.findById(request.fitnessId()).orElseThrow(() -> new IllegalArgumentException("Fitness not found")));
        entity.setRecordedByCoach(personRepository.findById(request.recordedByCoachId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        var entity = find(id);
        repository.delete(entity);
    }

    private FitnessRecord find(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("FitnessRecord not found"));
    }
}
