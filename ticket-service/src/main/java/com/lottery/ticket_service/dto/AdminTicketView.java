package com.lottery.ticket_service.dto;

import com.lottery.ticket_service.entity.Ticket;
import java.util.Arrays;
import java.util.List;

public record AdminTicketView(
        Long id, String ticketId, Long lotteryId, Long customerId,
        List<Integer> numbers) {

    public static AdminTicketView from(Ticket t) {
        List<Integer> numbers = Arrays.stream(t.getTicketNumber().split("-"))
                .map(Integer::parseInt).toList();
        return new AdminTicketView(t.getId(), t.getTicketId(),
                t.getLotteryId(), t.getCustomerId(), numbers);
    }
}