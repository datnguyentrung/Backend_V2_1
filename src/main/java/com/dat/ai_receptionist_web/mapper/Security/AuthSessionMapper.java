package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.AuthSession;
import com.dat.ai_receptionist_web.dto.Security.AuthSessionDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AuthSessionMapper {
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "activeUserPersonId", source = "activeUserPerson.userPersonId")
    AuthSessionDTO.Response toResponse(AuthSession entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "activeUserPerson", ignore = true)
    @Mapping(target = "refreshTokenHash", source = "refreshTokenHash")
    @Mapping(target = "deviceInfo", source = "deviceInfo")
    @Mapping(target = "platform", source = "platform")
    @Mapping(target = "fcmToken", source = "fcmToken")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "revoked", source = "revoked")
    @Mapping(target = "revokedAt", source = "revokedAt")
    @Mapping(target = "version", source = "version")
    void updateEntity(AuthSessionDTO.UpdateRequest request, @MappingTarget AuthSession entity);
}
