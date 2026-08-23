package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Finance.Wallet;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.enums.Finance.WalletStatus;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Finance.WalletRepository;
import com.dat.ai_receptionist_web.util.converter.NameConverter;
import com.dat.ai_receptionist_web.util.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;
    private final WalletRepository walletRepository;

    @Transactional
    public PersonDTO.Response create(PersonDTO.CreateRequest request) {
        if (request.nationalCode() != null && personRepository.existsByNationalCode(request.nationalCode())) {
            throw new BusinessException("National code already exists");
        }
        if (personRepository.existsByPersonCode(request.personCode())) {
            throw new BusinessException("Person code already exists");
        }
        Person person = personRepository.save(Person.builder()
                .fullName(NameConverter.formatVietnameseName(request.fullName()))
                .gender(request.gender())
                .birthDate(request.birthDate())
                .email(request.email())
                .nationalCode(request.nationalCode())
                .personCode(request.personCode().trim().toUpperCase())
                .belt(request.belt())
                .status(request.status())
                .startDate(request.startDate())
                .build());
        walletRepository.save(Wallet.builder()
                .person(person)
                .balance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build());
        return toResponse(person);
    }

    @Transactional(readOnly = true)
    public Page<PersonDTO.Response> search(String query, Pageable pageable) {
        Page<Person> people = query == null || query.isBlank()
                ? personRepository.findAll(pageable)
                : personRepository.findByFullNameContainingIgnoreCaseOrPersonCodeContainingIgnoreCase(
                        query.trim(), query.trim(), pageable);
        return people.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PersonDTO.Response get(UUID id) {
        return toResponse(personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person not found")));
    }

    private PersonDTO.Response toResponse(Person value) {
        return new PersonDTO.Response(value.getPersonId(), value.getFullName(), value.getGender(),
                value.getBirthDate(), value.getEmail(), value.getNationalCode(), value.getPersonCode(),
                value.getBelt(), value.getStatus(), value.getStartDate(), value.getFaceImagePath(),
                value.getCreatedAt(), value.getUpdatedAt());
    }
}
