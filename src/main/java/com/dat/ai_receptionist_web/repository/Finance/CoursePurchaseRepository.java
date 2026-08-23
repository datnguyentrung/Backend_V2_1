package com.dat.ai_receptionist_web.repository.Finance;

import com.dat.ai_receptionist_web.domain.Finance.CoursePurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface CoursePurchaseRepository extends JpaRepository<CoursePurchase, UUID> {
    Optional<CoursePurchase> findByDebitTransaction_WalletTransactionId(UUID transactionId);
}
