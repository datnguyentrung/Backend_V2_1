package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Branch;
import com.dat.ai_receptionist_web.dto.Core.BranchDTO;
import com.dat.ai_receptionist_web.mapper.Core.BranchMapper;
import com.dat.ai_receptionist_web.repository.Core.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        if (!branchRepository.existsById(idBranch)) {
            throw new IllegalArgumentException("Branch with id " + idBranch + " not found");
        }
        branchRepository.deleteById(idBranch);
    }
}
