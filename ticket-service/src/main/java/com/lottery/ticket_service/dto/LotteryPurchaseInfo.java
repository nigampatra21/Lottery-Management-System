package com.lottery.ticket_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LotteryPurchaseInfo(
        Long lotteryId, String status, BigDecimal ticketPrice, String currency,
        LocalDateTime salesStartAt, LocalDateTime salesEndAt, LocalDateTime drawAt,
        Long maxTickets, int numberCount, int minNumber, int maxNumber,
        boolean allowTicketCancellation, Integer cancellationCutoffMinutes,BigDecimal agentCommissionPercent) {}