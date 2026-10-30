package com.lottery.winner.dto;

import com.lottery.winner.entity.PrizeClaim;
import com.lottery.winner.entity.Winner;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record ClaimResponse(String claimReference, String status, String winnerId,
                            Long ticketId, BigDecimal prizeAmount,
                            Instant claimedAt, Instant verifiedAt) {

    public static ClaimResponse from(PrizeClaim c, Winner w) {
        return new ClaimResponse(c.getClaimReference(), c.getStatus(), w.getWinnerId(),
                w.getTicketId(), w.getPrizeAmount(),
                utc(c.getClaimedAt()), utc(c.getVerifiedAt()));
    }

    private static Instant utc(LocalDateTime t) {
        return t == null ? null : t.toInstant(ZoneOffset.UTC);
    }
}