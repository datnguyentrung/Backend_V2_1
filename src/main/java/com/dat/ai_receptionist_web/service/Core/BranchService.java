package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Branch;
import com.dat.ai_receptionist_web.repository.Core.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BranchService{
    private final BranchRepository branchRepository;

    public Branch getBranchById(Long idBranch) {
        return branchRepository.findById(idBranch)
                .orElseThrow(() -> new IllegalArgumentException("Branch with id " + idBranch + " not found"));
    }
}
