package com.dat.ai_receptionist_web.mapper.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import org.springframework.stereotype.Component;

@Component
public class PersonMapper {
    public PersonDTO.Response toResponse(Person entity) {
        if (entity == null) return null;
        return new PersonDTO.Response(entity.getPersonId(), entity.getFullName(), entity.getGender(), entity.getBirthDate(), entity.getEmail(), entity.getNationalCode(), entity.getPersonCode(), entity.getBelt(), entity.getStatus(), entity.getStartDate(), entity.getFaceImagePath(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public void updateEntity(PersonDTO.UpdateRequest request, Person entity) {
        entity.setFullName(request.fullName());
        entity.setGender(request.gender());
        entity.setBirthDate(request.birthDate());
        entity.setEmail(request.email());
        entity.setNationalCode(request.nationalCode());
        entity.setFaceImagePath(request.faceImagePath());
        entity.setPersonCode(request.personCode());
        entity.setBelt(request.belt());
        entity.setStatus(request.status());
        entity.setStartDate(request.startDate());
    }
}
