package com.dat.ai_receptionist_web.repository.Finance;

import com.dat.ai_receptionist_web.domain.Finance.WalletTransaction;
import com.dat.ai_receptionist_web.enums.Finance.WalletTransactionType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    Optional<WalletTransaction> findByTypeAndExternalReference(
            WalletTransactionType type, String externalReference);

    @EntityGraph(attributePaths = "wallet")
    @Query("select tx from WalletTransaction tx where tx.walletTransactionId = :id")
    Optional<WalletTransaction> findWithWalletById(@Param("id") UUID id);
}
