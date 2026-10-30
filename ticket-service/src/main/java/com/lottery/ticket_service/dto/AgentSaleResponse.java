package com.lottery.ticket_service.dto;

import java.math.BigDecimal;

public record AgentSaleResponse(
        TicketResponse ticket, Long customerId,
        BigDecimal commissionRate, BigDecimal commissionAmount,
        String commissionStatus) {}