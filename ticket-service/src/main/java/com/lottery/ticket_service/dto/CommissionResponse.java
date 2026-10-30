package com.lottery.ticket_service.dto;

import com.lottery.ticket_service.entity.AgentCommission;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record CommissionResponse(
        Long ticketId, BigDecimal ratePercent, BigDecimal ticketPrice,
        BigDecimal amount, String status, Instant earnedAt, Instant reversedAt) {

    public static CommissionResponse from(AgentCommission c) {
        return new CommissionResponse(c.getTicketId(), c.getRatePercent(),
                c.getTicketPrice(), c.getCommissionAmount(), c.getStatus().name(),
                utc(c.getEarnedAt()), utc(c.getReversedAt()));
    }

    private static Instant utc(LocalDateTime t) {
        return t == null ? null : t.toInstant(ZoneOffset.UTC);
    }
}