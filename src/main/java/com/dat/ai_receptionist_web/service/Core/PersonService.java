package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;

    @Transactional(readOnly = true)
    public PageResponse<PersonDTO.SearchItem> searchStudentsAndCoaches(String search, Pageable pageable) {
        String normalizedSearch = search == null || search.trim().isEmpty()
                ? null
                : "%" + search.trim().toLowerCase() + "%";
        Page<PersonRepository.PersonSearchProjection> people =
                personRepository.searchStudentsAndCoaches(normalizedSearch, pageable);
        return PageResponse.of(people, personMapper::toSearchItem);
    }
}
