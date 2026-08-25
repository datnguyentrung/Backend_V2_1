package com.dat.ai_receptionist_web.mapper.Core;

import com.dat.ai_receptionist_web.domain.Core.Branch;
import com.dat.ai_receptionist_web.dto.Core.BranchDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BranchMapper {
    public BranchDTO.Response toResponse(Branch entity) {
        if (entity == null) return null;
        return new BranchDTO.Response(entity.getBranchId(), entity.getName(), entity.getAddress(), entity.getHotline(), entity.getOpenedDate(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public Branch toEntity(BranchDTO.CreateRequest request) {
        Branch branch = new Branch();
        branch.setName(request.name());
        branch.setAddress(request.address());
        branch.setHotline(request.hotline());
        branch.setOpenedDate(request.openedDate());
        branch.setStatus(request.status());
        return branch;
    }

    public List<BranchDTO.Response> toResponseList(List<Branch> branches) {
        return branches.stream().map(this::toResponse).toList();
    }

    public void updateEntity(BranchDTO.UpdateRequest request, Branch entity) {
        entity.setName(request.name());
        entity.setAddress(request.address());
        entity.setHotline(request.hotline());
        entity.setOpenedDate(request.openedDate());
        entity.setStatus(request.status());
    }
}
