package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.RolePermission;
import com.dat.ai_receptionist_web.dto.Security.RolePermissionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RolePermissionMapper {
    @Mapping(target = "roleCode", source = "role.code")
    @Mapping(target = "permissionId", source = "permission.permissionId")
    @Mapping(target = "permissionCode", source = "permission.code")
    RolePermissionDTO.ItemResponse toResponse(RolePermission entity);
}
