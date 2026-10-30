package com.lottery.ticket_service.dto;

import com.lottery.ticket_service.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/** customerId is optional: leave it out for a walk-in customer. */
public record AgentSaleRequest(
        @NotNull @Schema(example = "4") Long lotteryId,
        @NotEmpty @Schema(example = "[3, 8, 15, 22, 31, 44]")
        List<@NotNull Integer> selectedNumbers,
        @NotNull PaymentMethod paymentMethod,
        @Positive @Schema(example = "2", description = "Omit for a walk-in customer")
        Long customerId) {}

