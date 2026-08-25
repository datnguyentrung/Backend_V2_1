package com.dat.ai_receptionist_web.dto.Catalog;

import jakarta.validation.constraints.*;
import com.dat.ai_receptionist_web.enums.Catalog.CoursePriceStatus;
import java.math.BigDecimal;
import java.util.UUID;

public final class CoursePriceDTO {
    private CoursePriceDTO() {
    }

    public record CreateRequest(@NotNull UUID courseId, int durationMonths, int sessionCount, @NotNull BigDecimal basePrice, @NotNull BigDecimal finalPrice, @NotNull CoursePriceStatus status) {
    }

    public record UpdateRequest(@NotNull UUID courseId, int durationMonths, int sessionCount, @NotNull BigDecimal basePrice, @NotNull BigDecimal finalPrice, @NotNull CoursePriceStatus status) {
    }

    public record Response(UUID coursePriceId, UUID courseId, int durationMonths, int sessionCount, BigDecimal basePrice, BigDecimal finalPrice, CoursePriceStatus status) {
    }
}
