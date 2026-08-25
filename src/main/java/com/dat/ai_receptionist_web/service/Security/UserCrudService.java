package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Security.UserDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Security.UserMapper;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCrudService {
    private final UserRepository repository;
    private final UserMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<UserDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public UserDTO.Response create(UserDTO.CreateRequest request) {
        User entity = new User();
        entity.setPhoneNumber(request.phoneNumber());
        entity.setPasswordHash(request.passwordHash());
        entity.setStatus(request.status());
        entity.setAuthorizationVersion(request.authorizationVersion());
        entity.setLastLoginAt(request.lastLoginAt());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public UserDTO.Response update(UUID id, UserDTO.UpdateRequest request) {
        var entity = find(id);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setStatus(UserStatus.DISABLED);
    }

    private User find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
