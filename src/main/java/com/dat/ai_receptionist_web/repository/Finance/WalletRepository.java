package com.dat.ai_receptionist_web.repository.Finance;

import com.dat.ai_receptionist_web.domain.Finance.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByPerson_PersonId(UUID personId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.person.personId = :personId")
    Optional<Wallet> findByPersonIdForUpdate(@Param("personId") UUID personId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.walletId = :walletId")
    Optional<Wallet> findByIdForUpdate(@Param("walletId") UUID walletId);
}
