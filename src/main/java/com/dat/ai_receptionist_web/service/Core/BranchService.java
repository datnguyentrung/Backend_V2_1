package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Branch;
import com.dat.ai_receptionist_web.dto.Core.BranchDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Core.BranchStatus;
import com.dat.ai_receptionist_web.mapper.Core.BranchMapper;
import com.dat.ai_receptionist_web.repository.Core.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BranchService{
    private final BranchRepository branchRepository;

    private final BranchMapper branchMapper;

    @Transactional(readOnly = true)
    public PageResponse<BranchDTO.Response> list(Pageable pageable) {
        return PageResponse.of(branchRepository.findAll(pageable), branchMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BranchDTO.Response get(Long id) {
        return branchMapper.toResponse(find(id));
    }

    @Transactional
    public BranchDTO.Response create(BranchDTO.CreateRequest request) {
        Branch branch = branchMapper.toEntity(request);
        return branchMapper.toResponse(branchRepository.save(branch));
    }

    @Transactional
    public BranchDTO.Response update(Long id, BranchDTO.UpdateRequest request) {
        Branch branch = find(id);
        branchMapper.updateEntity(request, branch);
        return branchMapper.toResponse(branchRepository.save(branch));
    }

    @Transactional
    public void delete(Long id) {
        Branch branch = find(id);
        branch.setStatus(BranchStatus.CLOSED);
    }

    public List<BranchDTO.Response> getAllBranches() {
        List<Branch> branches = branchRepository.findAll();
        return branchMapper.toResponseList(branches);
    }

    public BranchDTO.Response getBranchById(Long idBranch) {
        Branch branch = branchRepository.findById(idBranch)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Branch with id " + idBranch + " not found"
                        ));

        return branchMapper.toResponse(branch);
    }

    @Transactional
    public BranchDTO.Response createBranch(BranchDTO.CreateRequest request) {
        Branch branch = branchMapper.toEntity(request);
        Branch savedBranch = branchRepository.save(branch);
        return branchMapper.toResponse(savedBranch);
    }

    @Transactional
    public BranchDTO.Response updateBranch(Long idBranch, BranchDTO.CreateRequest request) {
        Optional<Branch> optionalBranch = branchRepository.findById(idBranch);
        if (optionalBranch.isEmpty()) {
            throw new IllegalArgumentException("Branch with id " + idBranch + " not found");
        }

        Branch branchToUpdate = optionalBranch.get();
        branchToUpdate.setName(request.name());
        branchToUpdate.setAddress(request.address());
        branchToUpdate.setHotline(request.hotline());
        branchToUpdate.setOpenedDate(request.openedDate());
        branchToUpdate.setStatus(request.status());

        Branch updatedBranch = branchRepository.save(branchToUpdate);
        return branchMapper.toResponse(updatedBranch);
    }

    @Transactional
    public void deleteBranch(Long idBranch) {
        delete(idBranch);
    }

    private Branch find(Long id) {
        return branchRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Branch not found"));
    }
}
