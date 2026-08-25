package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Branch;
import com.dat.ai_receptionist_web.dto.Core.BranchDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Core.BranchMapper;
import com.dat.ai_receptionist_web.repository.Core.BranchRepository;
import com.dat.ai_receptionist_web.enums.Core.BranchStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BranchCrudService {
    private final BranchRepository repository;
    private final BranchMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<BranchDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BranchDTO.Response get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public BranchDTO.Response create(BranchDTO.CreateRequest request) {
        Branch entity = new Branch();
        entity.setName(request.name());
        entity.setAddress(request.address());
        entity.setHotline(request.hotline());
        entity.setOpenedDate(request.openedDate());
        entity.setStatus(request.status());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public BranchDTO.Response update(Long id, BranchDTO.UpdateRequest request) {
        var entity = find(id);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        var entity = find(id);
        entity.setStatus(BranchStatus.CLOSED);
    }

    private Branch find(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Branch not found"));
    }
}
