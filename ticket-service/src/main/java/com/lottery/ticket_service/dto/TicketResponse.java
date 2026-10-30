package com.lottery.ticket_service.dto;

import com.lottery.ticket_service.entity.Ticket;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

public record TicketResponse(
        Long id, String ticketId, Long lotteryId, List<Integer> numbers,
        BigDecimal price, String status, Instant purchasedAt) {

    public static TicketResponse from(Ticket t) {
        List<Integer> numbers = Arrays.stream(t.getTicketNumber().split("-"))
                .map(Integer::parseInt).toList();
        return new TicketResponse(t.getId(), t.getTicketId(), t.getLotteryId(),
                numbers, t.getPrice(), t.getStatus().name(),
                t.getPurchasedAt().toInstant(ZoneOffset.UTC));
    }
}