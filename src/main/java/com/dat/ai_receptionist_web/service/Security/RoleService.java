package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
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

    public Role getRoleReferenceByCode(String roleCode) {
        return roleRepository.getReferenceById(roleCode);
    }
}
