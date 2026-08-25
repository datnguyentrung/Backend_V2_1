package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.*;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.RolePermissionDTO;
import com.dat.ai_receptionist_web.mapper.Security.RolePermissionMapper;
import com.dat.ai_receptionist_web.repository.Security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RolePermissionCrudService {
    private final RolePermissionRepository repository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<RolePermissionDTO.ItemResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RolePermissionDTO.ItemResponse get(String roleCode, Integer permissionId) {
        return mapper.toResponse(find(roleCode, permissionId));
    }

    @Transactional
    public RolePermissionDTO.ItemResponse create(RolePermissionDTO.CreateRequest request) {
        Role role = roleRepository.findById(request.roleCode()).orElseThrow(() -> new IllegalArgumentException("Role not found"));
        Permission permission = permissionRepository.findById(request.permissionId()).orElseThrow(() -> new IllegalArgumentException("Permission not found"));
        RolePermission entity = new RolePermission(new RolePermission.Key(role.getCode(), permission.getPermissionId()), role, permission);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(String roleCode, Integer permissionId) {
        repository.delete(find(roleCode, permissionId));
    }

    private RolePermission find(String roleCode, Integer permissionId) {
        return repository.findById(new RolePermission.Key(roleCode, permissionId)).orElseThrow(() -> new IllegalArgumentException("RolePermission not found"));
    }
}
