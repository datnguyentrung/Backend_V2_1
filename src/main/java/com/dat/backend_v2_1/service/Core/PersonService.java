package com.dat.backend_v2_1.service.Core;

import com.dat.backend_v2_1.dto.Core.PersonDTO;
import com.dat.backend_v2_1.dto.PageResponse;
import com.dat.backend_v2_1.repository.Core.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    @Transactional(readOnly = true)
    public PageResponse<PersonDTO.SearchItem> searchStudentsAndCoaches(String search, Pageable pageable) {
        String normalizedSearch = search == null || search.trim().isEmpty()
                ? null
                : "%" + search.trim().toLowerCase() + "%";
        Page<PersonRepository.PersonSearchProjection> people =
                personRepository.searchStudentsAndCoaches(normalizedSearch, pageable);
        return PageResponse.of(people, this::toSearchItem);
    }

    private PersonDTO.SearchItem toSearchItem(PersonRepository.PersonSearchProjection person) {
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
