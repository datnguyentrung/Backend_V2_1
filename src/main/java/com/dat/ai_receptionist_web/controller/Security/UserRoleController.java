package com.dat.ai_receptionist_web.controller.Security;

import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.UserRoleDTO;
import com.dat.ai_receptionist_web.service.Security.UserRoleService;
import com.dat.ai_receptionist_web.service.Security.UserRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user-roles")
@RequiredArgsConstructor
public class UserRoleController {
    private final UserRoleService userRoleService;
    private final UserRoleService Service;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).USER_ROLE_READ.getCode())")
    public PageResponse<UserRoleDTO.ItemResponse> list(Pageable pageable) { return Service.list(pageable); }

    @GetMapping("/{userId}/{roleCode}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).USER_ROLE_READ.getCode())")
    public UserRoleDTO.ItemResponse get(@PathVariable UUID userId, @PathVariable String roleCode) { return Service.get(userId, roleCode); }

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).USER_ROLE_CREATE.getCode())")
    public ResponseEntity<UserRoleDTO.Response> assignRole(@RequestBody @Valid UserRoleDTO.AssignRequest request) {
        return ResponseEntity.ok(userRoleService.assignRole(request));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).USER_ROLE_UPDATE.getCode())")
    public ResponseEntity<UserRoleDTO.Response> replaceRoles(@PathVariable UUID userId, @RequestBody @Valid UserRoleDTO.ReplaceRequest request) {
        return ResponseEntity.ok(userRoleService.replaceRoles(userId, request.getRoleCodes()));
    }

    @DeleteMapping("/{userId}/{roleCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).USER_ROLE_DELETE.getCode())")
    public void delete(@PathVariable UUID userId, @PathVariable String roleCode) { Service.delete(userId, roleCode); }
}
