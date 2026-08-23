package com.dat.ai_receptionist_web.dto.Security;

import jakarta.validation.constraints.*;

import java.util.Set;

public final class RolePermissionDTO {
    private RolePermissionDTO() {
    }

    public record ReplaceRequest(@NotEmpty Set<@NotBlank String> permissionCodes) {
    }

    public record Response(String roleCode, long permissionVersion, Set<String> permissionCodes) {
    }
}
