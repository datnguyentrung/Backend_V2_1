package com.dat.ai_receptionist_web.mapper.Core;

import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.dto.Core.UserPersonDTO;
import org.springframework.stereotype.Component;

@Component
public class UserPersonMapper {
    public UserPersonDTO.Response toResponse(UserPerson entity) {
        if (entity == null) return null;
        return new UserPersonDTO.Response(entity.getUserPersonId(), entity.getUser() == null ? null : entity.getUser().getUserId(), entity.getPerson() == null ? null : entity.getPerson().getPersonId(), entity.getRelationshipType(), entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public void updateEntity(UserPersonDTO.UpdateRequest request, UserPerson entity) {
        entity.setRelationshipType(request.relationshipType());
        entity.setActive(request.active());
    }
}
