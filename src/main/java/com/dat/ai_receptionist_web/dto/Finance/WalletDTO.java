package com.dat.ai_receptionist_web.dto.Finance;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import com.dat.ai_receptionist_web.enums.Finance.WalletStatus;
import java.math.BigDecimal;
import java.util.UUID;

public final class WalletDTO {
    private WalletDTO() {
    }

    public record CreateRequest(@NotNull UUID personId, @NotNull BigDecimal balance, @NotNull WalletStatus status) {
    }

    public record UpdateRequest(@NotNull UUID personId, @NotNull BigDecimal balance, @NotNull WalletStatus status) {
    }

    public record Response(UUID walletId, UUID personId, BigDecimal balance, WalletStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
