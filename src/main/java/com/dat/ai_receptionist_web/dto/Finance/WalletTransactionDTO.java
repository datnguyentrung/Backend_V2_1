package com.dat.ai_receptionist_web.dto.Finance;

import jakarta.validation.constraints.*;
import com.dat.ai_receptionist_web.enums.Finance.WalletTransactionDirection;
import com.dat.ai_receptionist_web.enums.Finance.WalletTransactionStatus;
import com.dat.ai_receptionist_web.enums.Finance.WalletTransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class WalletTransactionDTO {
    private WalletTransactionDTO() {
    }

    public record CreateRequest(@NotNull UUID walletId, @NotNull UUID createdByUserId, @NotNull UUID approvedByUserId, @NotNull WalletTransactionType type, @NotNull WalletTransactionDirection direction, @NotNull BigDecimal amount, @NotNull BigDecimal balanceBefore, @NotNull BigDecimal balanceAfter, @NotNull String externalReference, @NotNull LocalDateTime approvedAt, @NotNull String note, @NotNull WalletTransactionStatus status) {
    }

    public record UpdateRequest(@NotNull UUID walletId, @NotNull UUID createdByUserId, @NotNull UUID approvedByUserId, @NotNull WalletTransactionType type, @NotNull WalletTransactionDirection direction, @NotNull BigDecimal amount, @NotNull BigDecimal balanceBefore, @NotNull BigDecimal balanceAfter, @NotNull String externalReference, @NotNull LocalDateTime approvedAt, @NotNull String note, @NotNull WalletTransactionStatus status) {
    }

    public record Response(UUID walletTransactionId, UUID walletId, UUID createdByUserId, UUID approvedByUserId, WalletTransactionType type, WalletTransactionDirection direction, BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter, String externalReference, LocalDateTime approvedAt, String note, WalletTransactionStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
