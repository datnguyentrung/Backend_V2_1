package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.AuthSession;
import com.dat.ai_receptionist_web.dto.Security.AuthSessionDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Security.AuthSessionMapper;
import com.dat.ai_receptionist_web.repository.Security.AuthSessionRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import java.util.UUID;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthSessionCrudService {
    private final AuthSessionRepository repository;
    private final AuthSessionMapper mapper;
    private final UserRepository userRepository;
    private final UserPersonRepository userPersonRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuthSessionDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AuthSessionDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public AuthSessionDTO.Response create(AuthSessionDTO.CreateRequest request) {
        AuthSession entity = new AuthSession();
        entity.setUser(userRepository.findById(request.userId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
        entity.setActiveUserPerson(userPersonRepository.findById(request.activeUserPersonId()).orElseThrow(() -> new IllegalArgumentException("UserPerson not found")));
        entity.setRefreshTokenHash(request.refreshTokenHash());
        entity.setDeviceInfo(request.deviceInfo());
        entity.setPlatform(request.platform());
        entity.setFcmToken(request.fcmToken());
        entity.setExpiresAt(request.expiresAt());
        entity.setRevoked(request.revoked());
        entity.setRevokedAt(request.revokedAt());
        entity.setVersion(request.version());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public AuthSessionDTO.Response update(UUID id, AuthSessionDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setUser(userRepository.findById(request.userId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
        entity.setActiveUserPerson(userPersonRepository.findById(request.activeUserPersonId()).orElseThrow(() -> new IllegalArgumentException("UserPerson not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setRevoked(true);
        entity.setRevokedAt(LocalDateTime.now());
    }

    private AuthSession find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("AuthSession not found"));
    }
}
