package com.dat.ai_receptionist_web.controller.Core;

import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.service.Core.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
public class PersonController {
    private final PersonService personService;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).PERSON_READ.getCode())")
    public Page<PersonDTO.Response> search(@RequestParam(required = false) String search,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return personService.search(search, PageRequest.of(page, size, Sort.by("fullName")));
    }

    @GetMapping("/{personId}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).PERSON_READ.getCode())")
    public PersonDTO.Response get(@PathVariable UUID personId) {
        return personService.get(personId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).PERSON_CREATE.getCode())")
    public PersonDTO.Response create(@Valid @RequestBody PersonDTO.CreateRequest request) {
        return personService.create(request);
    }
}
