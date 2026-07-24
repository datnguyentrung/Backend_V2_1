package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoachRepository extends JpaRepository<Coach, UUID> {

    boolean existsByStaffCode(String staffCode);

    Optional<Coach> findByStaffCode(String staffCode);
}
