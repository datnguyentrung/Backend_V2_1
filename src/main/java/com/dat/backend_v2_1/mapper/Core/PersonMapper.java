package com.dat.backend_v2_1.mapper.Core;

import com.dat.backend_v2_1.dto.Core.PersonDTO;
import com.dat.backend_v2_1.repository.Core.PersonRepository;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PersonMapper {

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
