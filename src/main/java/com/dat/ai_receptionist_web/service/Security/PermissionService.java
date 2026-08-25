package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Permission;
import com.dat.ai_receptionist_web.dto.Security.PermissionDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Security.PermissionMapper;
import com.dat.ai_receptionist_web.repository.Security.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PermissionService {
    private final PermissionRepository repository;
    private final PermissionMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<PermissionDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PermissionDTO.Response get(Integer id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public PermissionDTO.Response create(PermissionDTO.CreateRequest request) {
        Permission entity = new Permission();
        entity.setCode(request.code());
        entity.setModel(request.model());
        entity.setAction(request.action());
        entity.setDescription(request.description());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public PermissionDTO.Response update(Integer id, PermissionDTO.UpdateRequest request) {
        var entity = find(id);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Integer id) {
        var entity = find(id);
        repository.delete(entity);
    }

    private Permission find(Integer id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Permission not found"));
    }
}
