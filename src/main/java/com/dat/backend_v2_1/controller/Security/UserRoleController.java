package com.dat.backend_v2_1.controller.Security;

import com.dat.backend_v2_1.dto.Security.UserRoleDTO;
import com.dat.backend_v2_1.service.Security.UserRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user-roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService userRoleService;

    @PreAuthorize("@securityRule.isManagerSenior(authentication)")
    @PostMapping
    public ResponseEntity<UserRoleDTO.Response> assignRole(
            @RequestBody @Valid UserRoleDTO.AssignRequest request
    ) {
        return ResponseEntity.ok(userRoleService.assignRole(request));
    }
}
