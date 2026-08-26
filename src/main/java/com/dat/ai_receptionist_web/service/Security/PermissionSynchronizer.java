package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Permission;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class PermissionSynchronizer implements ApplicationRunner {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        Map<String, Permission> dbPermissions =
                permissionRepository.findAll().stream()
                        .collect(Collectors.toMap(
                                Permission::getCode,
                                Function.identity()
                        ));

        Map<String, PermissionDefinition> definitions =
                Arrays.stream(PermissionDefinition.values())
                        .collect(Collectors.toMap(
                                PermissionDefinition::getCode,
                                Function.identity()
                        ));

        List<Permission> toSave = new ArrayList<>();

        int inserted = 0;
        int updated = 0;

        // INSERT + UPDATE
        for (PermissionDefinition definition : PermissionDefinition.values()) {

            Permission existing = dbPermissions.get(definition.getCode());

            if (existing == null) {
                toSave.add(
                        Permission.builder()
                                .code(definition.getCode())
                                .model(definition.getModel())
                                .action(definition.getAction())
                                .description(definition.getDescription())
                                .build()
                );

                inserted++;
                continue;
            }

            boolean changed =
                    !Objects.equals(existing.getModel(), definition.getModel())
                            || !Objects.equals(existing.getAction(), definition.getAction())
                            || !Objects.equals(existing.getDescription(), definition.getDescription());

            if (changed) {
                existing.setModel(definition.getModel());
                existing.setAction(definition.getAction());
                existing.setDescription(definition.getDescription());

                toSave.add(existing);
                updated++;
            }
        }

        if (!toSave.isEmpty()) {
            permissionRepository.saveAll(toSave);
        }

        // DELETE permission không còn trong enum
        Set<String> obsoleteCodes = dbPermissions.keySet().stream()
                .filter(code -> !definitions.containsKey(code))
                .collect(Collectors.toSet());

        int deleted = 0;

        if (!obsoleteCodes.isEmpty()) {
            deleted = permissionRepository.deleteByCodes(obsoleteCodes);
        }

        log.info(
                "Permission sync completed: total={}, inserted={}, updated={}, deleted={}",
                definitions.size(),
                inserted,
                updated,
                deleted
        );
    }
}