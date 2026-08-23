package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public Role getRole(String code) {
        return roleRepository.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + code));
    }
}
