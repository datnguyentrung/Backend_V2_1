package com.dat.ai_receptionist_web.controller.Security;

import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.RolePermissionDTO;
import com.dat.ai_receptionist_web.service.Security.RolePermissionService;
import com.dat.ai_receptionist_web.service.Security.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RolePermissionController {
    private final RolePermissionService rolePermissionService;
    private final RolePermissionService Service;

    @GetMapping("/api/v1/role-permissions")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).ROLE_PERMISSION_READ.getCode())")
    public PageResponse<RolePermissionDTO.ItemResponse> list(Pageable pageable) { return Service.list(pageable); }

    @GetMapping("/api/v1/role-permissions/{roleCode}/{permissionId}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).ROLE_PERMISSION_READ.getCode())")
    public RolePermissionDTO.ItemResponse get(@PathVariable String roleCode, @PathVariable Integer permissionId) { return Service.get(roleCode, permissionId); }

    @PostMapping("/api/v1/role-permissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).ROLE_PERMISSION_CREATE.getCode())")
    public RolePermissionDTO.ItemResponse create(@Valid @RequestBody RolePermissionDTO.CreateRequest request) { return Service.create(request); }

    @DeleteMapping("/api/v1/role-permissions/{roleCode}/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).ROLE_PERMISSION_DELETE.getCode())")
    public void delete(@PathVariable String roleCode, @PathVariable Integer permissionId) { Service.delete(roleCode, permissionId); }

    @PutMapping("/api/v1/roles/{roleCode}/permissions")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).ROLE_PERMISSION_UPDATE.getCode())")
    public RolePermissionDTO.Response replace(@PathVariable String roleCode, @Valid @RequestBody RolePermissionDTO.ReplaceRequest request) {
        return rolePermissionService.replace(roleCode, request.permissionCodes());
    }
}
