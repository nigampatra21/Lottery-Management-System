package com.lottery.draw.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public record LotteryPurchaseInfo(
        Long lotteryId, String status, BigDecimal ticketPrice, String currency,
        LocalDateTime salesStartAt, LocalDateTime salesEndAt, LocalDateTime drawAt,
        Long maxTickets, int numberCount, int minNumber, int maxNumber,
        boolean allowTicketCancellation, Integer cancellationCutoffMinutes,BigDecimal agentCommissionPercent) {}