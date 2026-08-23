package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.repository.Security.PermissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PermissionSynchronizerTest {
    @Test
    void repeatedRunsOnlyUseIdempotentUpsertsAndNeverDelete() {
        PermissionRepository repository = mock(PermissionRepository.class);
        when(repository.findAllCodes()).thenReturn(Set.of());
        PermissionSynchronizer synchronizer = new PermissionSynchronizer(repository);

        synchronizer.run(new DefaultApplicationArguments());
        synchronizer.run(new DefaultApplicationArguments());

        verify(repository, times(PermissionDefinition.values().length * 2))
                .upsert(anyString(), anyString(), anyString(), anyString());
        verify(repository, never()).deleteAll();
    }
}
