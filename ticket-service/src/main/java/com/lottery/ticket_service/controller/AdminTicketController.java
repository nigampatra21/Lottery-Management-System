package com.lottery.ticket_service.controller;

import com.lottery.ticket_service.dto.AdminTicketPage;
import com.lottery.ticket_service.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tickets")
@Tag(name = "Tickets (admin)", description = "Used by winner-service. Role: ADMIN")
public class AdminTicketController {

    private final TicketService ticketService;

    public AdminTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Issued tickets of a lottery, in pages",
            description = "Cancelled tickets are never returned. "
                    + "Use nextAfterId from the response as afterId for the next page")
    @GetMapping("/lottery/{lotteryId}")
    public AdminTicketPage issuedForLottery(
            @PathVariable Long lotteryId,
            @RequestParam(defaultValue = "0") Long afterId,
            @RequestParam(defaultValue = "500") int limit) {
        return ticketService.issuedTicketsForLottery(lotteryId, afterId, limit);
    }
}