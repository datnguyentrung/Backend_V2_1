package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.dto.Core.UserPersonDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Core.UserPersonMapper;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPersonCrudService {
    private final UserPersonRepository repository;
    private final UserPersonMapper mapper;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;

    @Transactional(readOnly = true)
    public PageResponse<UserPersonDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserPersonDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public UserPersonDTO.Response create(UserPersonDTO.CreateRequest request) {
        UserPerson entity = new UserPerson();
        entity.setUser(userRepository.findById(request.userId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
        entity.setPerson(personRepository.findById(request.personId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        entity.setRelationshipType(request.relationshipType());
        entity.setActive(request.active());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public UserPersonDTO.Response update(UUID id, UserPersonDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setUser(userRepository.findById(request.userId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
        entity.setPerson(personRepository.findById(request.personId()).orElseThrow(() -> new IllegalArgumentException("Person not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setActive(false);
    }

    private UserPerson find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("UserPerson not found"));
    }
}
