package com.dat.backend_v2_1.service.Security;

import com.dat.backend_v2_1.domain.Security.Role;
import com.dat.backend_v2_1.repository.Security.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    public Role getRoleById(String roleCode){
        return roleRepository.findById(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Role with code " + roleCode + " not found"));
    }

    public Role createRole(Role role){
        return roleRepository.save(role);
    }

    public Role getRoleByCode(String roleCode){
        return roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Role with code " + roleCode + " not found"));
    }
}
