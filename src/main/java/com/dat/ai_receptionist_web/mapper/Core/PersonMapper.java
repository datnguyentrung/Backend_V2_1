package com.dat.ai_receptionist_web.mapper.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PersonMapper {

    PersonDTO.PersonResponse toPersonResponse(Person person);

    default PersonDTO.SearchItem toSearchItem(PersonRepository.PersonSearchProjection person) {
        if (person == null) {
            return null;
        }
        return PersonDTO.SearchItem.builder()
                .personId(person.getPersonId())
                .fullName(person.getFullName())
                .birthDate(person.getBirthDate())
                .belt(person.getBelt())
                .personType(person.getPersonType())
                .code(person.getCode())
                .status(person.getStatus())
                .build();
    }
}
