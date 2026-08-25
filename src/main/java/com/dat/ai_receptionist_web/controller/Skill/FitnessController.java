package com.dat.ai_receptionist_web.controller.Skill;

import com.dat.ai_receptionist_web.dto.Skill.FitnessDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Skill.FitnessCrudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fitness")
@RequiredArgsConstructor
public class FitnessController {
    private final FitnessCrudService service;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).FITNESS_READ.getCode())")
    public PageResponse<FitnessDTO.Response> list(Pageable pageable) { return service.list(pageable); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).FITNESS_READ.getCode())")
    public FitnessDTO.Response get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).FITNESS_CREATE.getCode())")
    public FitnessDTO.Response create(@Valid @RequestBody FitnessDTO.CreateRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).FITNESS_UPDATE.getCode())")
    public FitnessDTO.Response update(@PathVariable Long id, @Valid @RequestBody FitnessDTO.UpdateRequest request) { return service.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).FITNESS_DELETE.getCode())")
    public void delete(@PathVariable Long id) { service.delete(id); }
}
