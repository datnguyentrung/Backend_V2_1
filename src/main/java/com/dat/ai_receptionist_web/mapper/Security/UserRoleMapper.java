package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.UserRole;
import com.dat.ai_receptionist_web.dto.Security.UserRoleDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserRoleMapper {
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "roleCode", source = "role.code")
    UserRoleDTO.ItemResponse toResponse(UserRole entity);
}
