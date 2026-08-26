package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Permission;
import com.dat.ai_receptionist_web.enums.Security.PermissionAction;
import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.repository.Security.PermissionRepository;
import com.dat.ai_receptionist_web.repository.Security.RolePermissionRepository;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

class PermissionSynchronizerTest {
    private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
    private final RolePermissionRepository rolePermissionRepository = mock(RolePermissionRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PermissionSynchronizer synchronizer =
            new PermissionSynchronizer(permissionRepository, rolePermissionRepository, roleRepository);

    @Test
    void repeatedRunsDoNotWriteWhenDefinitionsAlreadyMatch() {
        when(permissionRepository.findAll())
                .thenReturn(List.of())
                .thenReturn(existingPermissions());

        synchronizer.run(new DefaultApplicationArguments());
        synchronizer.run(new DefaultApplicationArguments());

        verify(permissionRepository, times(2)).findAll();
        verify(permissionRepository, times(1)).saveAll(anyList());
        verify(permissionRepository, never()).deleteByCodes(anySet());
        verifyNoInteractions(rolePermissionRepository, roleRepository);
    }

    @Test
    void obsoletePermissionsRemoveRolePermissionsAndIncrementAffectedRolesOnce() {
        String obsoleteCode = "OBSOLETE_PERMISSION";
        Permission obsolete = Permission.builder()
                .code(obsoleteCode)
                .model("OBSOLETE")
                .action(PermissionAction.READ)
                .description("Obsolete")
                .build();
        when(permissionRepository.findAll()).thenReturn(withObsoletePermission(obsolete));
        when(rolePermissionRepository.findRoleCodesByPermissionCodeIn(Set.of(obsoleteCode)))
                .thenReturn(Set.of("SUPER_ADMIN", "SYSTEM_ADMIN"));
        when(permissionRepository.deleteByCodes(Set.of(obsoleteCode))).thenReturn(1);

        synchronizer.run(new DefaultApplicationArguments());

        verify(rolePermissionRepository).deleteByPermissionCodeIn(Set.of(obsoleteCode));
        verify(roleRepository).incrementPermissionVersion("SUPER_ADMIN");
        verify(roleRepository).incrementPermissionVersion("SYSTEM_ADMIN");
        verify(permissionRepository).deleteByCodes(Set.of(obsoleteCode));
    }

    private List<Permission> existingPermissions() {
        return Arrays.stream(PermissionDefinition.values())
                .map(this::permission)
                .toList();
    }

    private List<Permission> withObsoletePermission(Permission obsolete) {
        return java.util.stream.Stream.concat(existingPermissions().stream(), java.util.stream.Stream.of(obsolete))
                .toList();
    }

    private Permission permission(PermissionDefinition definition) {
        return Permission.builder()
                .code(definition.getCode())
                .model(definition.getModel())
                .action(definition.getAction())
                .description(definition.getDescription())
                .build();
    }
}
