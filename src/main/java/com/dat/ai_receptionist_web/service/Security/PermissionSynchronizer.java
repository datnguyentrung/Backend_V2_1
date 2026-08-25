package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.repository.Security.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class PermissionSynchronizer implements ApplicationRunner {
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Set<String> definedCodes = new HashSet<>();

        for (PermissionDefinition definition : PermissionDefinition.values()) {
            definedCodes.add(definition.getCode());
            permissionRepository.upsert(definition.getCode(), definition.getModel(),
                    definition.getAction().name(), definition.getDescription());
        }

        permissionRepository.findAllCodes().stream()
                .filter(code -> !definedCodes.contains(code))
                .sorted()
                .forEach(code -> log.warn("Permission {} exists in DB but not in PermissionDefinition; it was not deleted", code));
    }
}
