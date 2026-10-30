package com.lottery.winner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminTicketView(Long id, String ticketId, Long lotteryId,
                              Long customerId, List<Integer> numbers) {}