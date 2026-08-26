package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.enums.Security.SystemRoleDefinition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class SystemRolePermissionSynchronizerTest {
    private final RolePermissionService rolePermissionService = mock(RolePermissionService.class);
    private final SystemRolePermissionSynchronizer synchronizer =
            new SystemRolePermissionSynchronizer(rolePermissionService);

    @Test
    void superAdminReceivesAllDefinedPermissions() {
        Set<String> allPermissions = Arrays.stream(PermissionDefinition.values())
                .map(PermissionDefinition::getCode)
                .collect(Collectors.toSet());

        assertThat(synchronizer.getPermissionsForRole(SystemRoleDefinition.SUPER_ADMIN))
                .containsExactlyInAnyOrderElementsOf(allPermissions);
    }

    @Test
    void systemAdminReceivesAllExceptWalletRestrictedPermissions() {
        Set<String> permissions = synchronizer.getPermissionsForRole(SystemRoleDefinition.SYSTEM_ADMIN);

        assertThat(permissions).doesNotContainAnyElementsOf(synchronizer.getSystemAdminExcludedPermissions());
        assertThat(permissions.size()).isEqualTo(
                PermissionDefinition.values().length - synchronizer.getSystemAdminExcludedPermissions().size()
        );
        assertThat(synchronizer.getSystemAdminExcludedPermissions()).containsExactlyInAnyOrder(
                PermissionDefinition.WALLET_READ.getCode(),
                PermissionDefinition.WALLET_CREATE.getCode(),
                PermissionDefinition.WALLET_UPDATE.getCode(),
                PermissionDefinition.WALLET_DELETE.getCode(),
                PermissionDefinition.WALLET_TRANSACTION_READ.getCode(),
                PermissionDefinition.WALLET_TRANSACTION_CREATE.getCode(),
                PermissionDefinition.WALLET_TRANSACTION_UPDATE.getCode(),
                PermissionDefinition.WALLET_TRANSACTION_DELETE.getCode(),
                PermissionDefinition.WALLET_TOP_UP_CREATE.getCode(),
                PermissionDefinition.WALLET_REFUND_CREATE.getCode()
        );
    }

    @Test
    void runReplacesAllSystemRolePermissionsInOneBulkCall() {
        when(rolePermissionService.replaceAll(anyMap()))
                .thenReturn(new RolePermissionService.BulkSyncResult(0, 0));

        synchronizer.run(mock(org.springframework.boot.ApplicationArguments.class));

        @SuppressWarnings("unchecked")
        var desired = ArgumentCaptor.forClass(Map.class);
        verify(rolePermissionService).replaceAll(desired.capture());
        assertThat(desired.getValue().keySet()).containsExactlyInAnyOrder(
                SystemRoleDefinition.SUPER_ADMIN.getCode(),
                SystemRoleDefinition.SYSTEM_ADMIN.getCode()
        );
    }
}
