package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.enums.Security.SystemRoleDefinition;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class SystemRoleSynchronizer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {

        for (SystemRoleDefinition definition
                : SystemRoleDefinition.values()) {

            Role role = roleRepository
                    .findById(definition.getCode())
                    .orElseGet(() -> new Role(
                            definition.getCode(),
                            definition.getName(),
                            definition.getDescription(),
                            0L
                    ));

            role.setName(definition.getName());
            role.setDescription(definition.getDescription());

            roleRepository.save(role);
        }
    }
}