package com.dat.ai_receptionist_web.mapper.Core;

import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.dto.Core.UserPersonDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserPersonMapper {
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "personId", source = "person.personId")
    UserPersonDTO.Response toResponse(UserPerson entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "person", ignore = true)
    @Mapping(target = "relationshipType", source = "relationshipType")
    @Mapping(target = "active", source = "active")
    void updateEntity(UserPersonDTO.UpdateRequest request, @MappingTarget UserPerson entity);
}
