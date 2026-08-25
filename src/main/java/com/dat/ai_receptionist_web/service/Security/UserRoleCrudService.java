package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.*;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.UserRoleDTO;
import com.dat.ai_receptionist_web.mapper.Security.UserRoleMapper;
import com.dat.ai_receptionist_web.repository.Security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRoleCrudService {
    private final UserRoleRepository repository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<UserRoleDTO.ItemResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserRoleDTO.ItemResponse get(UUID userId, String roleCode) {
        return mapper.toResponse(find(userId, roleCode));
    }

    @Transactional
    public UserRoleDTO.ItemResponse create(UserRoleDTO.CreateRequest request) {
        User user = userRepository.findById(request.userId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Role role = roleRepository.findById(request.roleCode()).orElseThrow(() -> new IllegalArgumentException("Role not found"));
        UserRole entity = new UserRole(new UserRole.Key(user.getUserId(), role.getCode()), user, role);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID userId, String roleCode) {
        repository.delete(find(userId, roleCode));
    }

    private UserRole find(UUID userId, String roleCode) {
        return repository.findById(new UserRole.Key(userId, roleCode)).orElseThrow(() -> new IllegalArgumentException("UserRole not found"));
    }
}
