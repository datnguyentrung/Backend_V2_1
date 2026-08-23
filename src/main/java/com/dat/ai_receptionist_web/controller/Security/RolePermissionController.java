package com.dat.ai_receptionist_web.controller.Security;

import com.dat.ai_receptionist_web.dto.Security.RolePermissionDTO;
import com.dat.ai_receptionist_web.service.Security.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/roles/{roleCode}/permissions")
@RequiredArgsConstructor
public class RolePermissionController {
    private final RolePermissionService rolePermissionService;

    @PutMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).ROLE_PERMISSION_UPDATE.getCode())")
    public RolePermissionDTO.Response replace(@PathVariable String roleCode,
                                              @Valid @RequestBody RolePermissionDTO.ReplaceRequest request) {
        return rolePermissionService.replace(roleCode, request.permissionCodes());
    }
}
