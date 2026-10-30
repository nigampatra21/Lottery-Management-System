package com.lottery.winner.dto;

import com.lottery.winner.entity.Winner;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

public record WinnerResponse(String winnerId, Long ticketId, Long lotteryId,
                             Long customerId, Long prizeTierId,
                             BigDecimal prizeAmount, String status,
                             Instant declaredAt) {

    public static WinnerResponse from(Winner w) {
        return new WinnerResponse(w.getWinnerId(), w.getTicketId(), w.getLotteryId(),
                w.getCustomerId(), w.getPrizeTierId(), w.getPrizeAmount(),
                w.getStatus(), w.getDeclaredAt().toInstant(ZoneOffset.UTC));
    }
}