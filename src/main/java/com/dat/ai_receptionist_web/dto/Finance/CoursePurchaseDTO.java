package com.dat.ai_receptionist_web.dto.Finance;

import jakarta.validation.constraints.*;
import java.util.UUID;

public final class CoursePurchaseDTO {
    private CoursePurchaseDTO() {
    }

    public record CreateRequest(@NotNull UUID studentPersonId, @NotNull UUID coursePriceId, @NotNull UUID debitTransactionId) {
    }

    public record UpdateRequest(@NotNull UUID studentPersonId, @NotNull UUID coursePriceId, @NotNull UUID debitTransactionId) {
    }

    public record Response(UUID coursePurchaseId, UUID studentPersonId, UUID coursePriceId, UUID debitTransactionId) {
    }
}
