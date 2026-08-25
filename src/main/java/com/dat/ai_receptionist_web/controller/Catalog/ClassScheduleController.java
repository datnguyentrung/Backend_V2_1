package com.dat.ai_receptionist_web.controller.Catalog;

import com.dat.ai_receptionist_web.dto.Catalog.ClassScheduleDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Catalog.ClassScheduleCrudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/class-schedules")
@RequiredArgsConstructor
public class ClassScheduleController {
    private final ClassScheduleCrudService service;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).CLASS_SCHEDULE_READ.getCode())")
    public PageResponse<ClassScheduleDTO.Response> list(Pageable pageable) { return service.list(pageable); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).CLASS_SCHEDULE_READ.getCode())")
    public ClassScheduleDTO.Response get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).CLASS_SCHEDULE_CREATE.getCode())")
    public ClassScheduleDTO.Response create(@Valid @RequestBody ClassScheduleDTO.CreateRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).CLASS_SCHEDULE_UPDATE.getCode())")
    public ClassScheduleDTO.Response update(@PathVariable UUID id, @Valid @RequestBody ClassScheduleDTO.UpdateRequest request) { return service.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).CLASS_SCHEDULE_DELETE.getCode())")
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
