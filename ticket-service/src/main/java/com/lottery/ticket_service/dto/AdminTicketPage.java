package com.lottery.ticket_service.dto;

import java.util.List;

/** nextAfterId is null on the last page. */
public record AdminTicketPage(List<AdminTicketView> tickets, Long nextAfterId) {}