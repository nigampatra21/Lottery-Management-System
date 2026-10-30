package com.lottery.ticket_service.dto;

import com.lottery.ticket_service.entity.TicketTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

public record TransactionResponse(
        Long id, String reference, String type, BigDecimal amount,
        String status, String paymentMethod, Instant createdAt) {

    public static TransactionResponse from(TicketTransaction t) {
        return new TransactionResponse(t.getId(), t.getTransactionReference(),
                t.getTransactionType().name(), t.getAmount(), t.getStatus().name(),
                t.getPaymentMethod() == null ? null : t.getPaymentMethod().name(),
                t.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
}