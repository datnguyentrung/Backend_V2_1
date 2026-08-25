package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.dto.Security.RoleDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Security.RoleMapper;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleCrudService {
    private final RoleRepository repository;
    private final RoleMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<RoleDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RoleDTO.Response get(String id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public RoleDTO.Response create(RoleDTO.CreateRequest request) {
        Role entity = new Role();
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setPermissionVersion(request.permissionVersion());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public RoleDTO.Response update(String id, RoleDTO.UpdateRequest request) {
        var entity = find(id);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        var entity = find(id);
        repository.delete(entity);
    }

    private Role find(String id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Role not found"));
    }
}
