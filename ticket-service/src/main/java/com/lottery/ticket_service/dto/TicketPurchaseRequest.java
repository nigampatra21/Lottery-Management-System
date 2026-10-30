package com.lottery.ticket_service.dto;

import com.lottery.ticket_service.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TicketPurchaseRequest(

        @NotNull @Schema(example = "4") Long lotteryId,
        @NotEmpty @Schema(example = "[5, 12, 23, 34, 41, 47]")
        List<@NotNull Integer> selectedNumbers) {}