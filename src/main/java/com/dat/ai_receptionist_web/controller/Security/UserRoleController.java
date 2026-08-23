package com.dat.ai_receptionist_web.controller.Security;

import com.dat.ai_receptionist_web.dto.Security.UserRoleDTO;
import com.dat.ai_receptionist_web.service.Security.UserRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user-roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService userRoleService;

    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).USER_ROLE_UPDATE.getCode())")
    @PostMapping
    public ResponseEntity<UserRoleDTO.Response> assignRole(
            @RequestBody @Valid UserRoleDTO.AssignRequest request
    ) {
        return ResponseEntity.ok(userRoleService.assignRole(request));
    }

    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).USER_ROLE_UPDATE.getCode())")
    @PutMapping("/{userId}")
    public ResponseEntity<UserRoleDTO.Response> replaceRoles(
            @PathVariable UUID userId, @RequestBody @Valid UserRoleDTO.ReplaceRequest request) {
        return ResponseEntity.ok(userRoleService.replaceRoles(userId, request.getRoleCodes()));
    }
}
