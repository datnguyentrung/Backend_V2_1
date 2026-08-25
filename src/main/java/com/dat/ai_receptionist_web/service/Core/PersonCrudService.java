package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.enums.Core.PersonStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonCrudService {
    private final PersonRepository repository;
    private final PersonMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<PersonDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PersonDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public PersonDTO.Response create(PersonDTO.CreateRequest request) {
        Person entity = new Person();
        entity.setFullName(request.fullName());
        entity.setGender(request.gender());
        entity.setBirthDate(request.birthDate());
        entity.setEmail(request.email());
        entity.setNationalCode(request.nationalCode());
        entity.setFaceImagePath(request.faceImagePath());
        entity.setPersonCode(request.personCode());
        entity.setBelt(request.belt());
        entity.setStatus(request.status());
        entity.setStartDate(request.startDate());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public PersonDTO.Response update(UUID id, PersonDTO.UpdateRequest request) {
        var entity = find(id);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setStatus(PersonStatus.INACTIVE);
    }

    private Person find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Person not found"));
    }
}
