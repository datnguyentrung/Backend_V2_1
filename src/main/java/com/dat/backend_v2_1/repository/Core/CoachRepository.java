package com.dat.backend_v2_1.repository.Core;

import com.dat.backend_v2_1.domain.Core.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CoachRepository extends JpaRepository<Coach, UUID> {

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByStaffCode(String staffCode);
}
