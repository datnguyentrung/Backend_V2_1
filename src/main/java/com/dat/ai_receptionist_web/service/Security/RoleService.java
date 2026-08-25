package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.RoleDTO;
import com.dat.ai_receptionist_web.mapper.Security.RoleMapper;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    private final RoleMapper roleMapper;

    private final RolePermissionService rolePermissionService;

    @Transactional(readOnly = true)
    public PageResponse<RoleDTO.Response> list(Pageable pageable) {
        return PageResponse.of(roleRepository.findAll(pageable), roleMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RoleDTO.Response get(String id) {
        return roleMapper.toResponse(getRole(id));
    }

    @Transactional
    public RoleDTO.Response create(RoleDTO.CreateRequest request) {
        Role role = new Role();
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        role.setPermissionVersion(request.permissionVersion());
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleDTO.Response update(String id, RoleDTO.UpdateRequest request) {
        Role role = getRole(id);
        roleMapper.updateEntity(request, role);
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Transactional
    public void delete(String id) {
        roleRepository.delete(getRole(id));
    }

    public Role getRole(String code) {
        return roleRepository.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + code));
    }

    public boolean exists(String code) {
        return roleRepository.existsById(code);
    }

    @Transactional
    public Role createRole(RoleDTO.CreateRequest request) {
        String code = request.code();
        String name = request.name();
        String description = request.description();
        if (exists(code)) {
            throw new IllegalArgumentException("Role already exists: " + code);
        }
        Role role = roleRepository.save(roleMapper.toEntity(request));

        rolePermissionService.replace(role.getCode(), Set.of(description));

        return roleRepository.save(role);
    }

    @Transactional
    public Role updateRole(String code, RoleDTO.UpdateRequest request) {
        Role role = getRole(code);
        roleMapper.updateEntity(request, role);
        return roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(String code) {
        Role role = getRole(code);
        roleRepository.delete(role);
    }
}
