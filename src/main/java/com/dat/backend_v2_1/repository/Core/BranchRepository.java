package com.dat.backend_v2_1.repository.Core;

import com.dat.backend_v2_1.domain.Core.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
}
