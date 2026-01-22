package com.dat.backend_v2_1.service.Core;

import com.dat.backend_v2_1.domain.Core.Branch;
import com.dat.backend_v2_1.repository.Core.BranchRepository;
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
