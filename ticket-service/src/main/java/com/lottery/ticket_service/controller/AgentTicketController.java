package com.lottery.ticket_service.controller;

import com.lottery.ticket_service.dto.AgentSaleRequest;
import com.lottery.ticket_service.dto.AgentSaleResponse;
import com.lottery.ticket_service.dto.CommissionSummary;
import com.lottery.ticket_service.security.AuthenticatedUser;
import com.lottery.ticket_service.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "Agent sales", description = "Sell tickets and view commissions. Role: AGENT")
public class AgentTicketController {

    private final TicketService ticketService;

    public AgentTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Sell a ticket",
            description = "Leave customerId out for a walk-in customer. "
                    + "The commission is calculated by the server")
    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public AgentSaleResponse sell(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody AgentSaleRequest req) {
        return ticketService.sell(user.userId(), authHeader, req);
    }

    @Operation(summary = "List my sales")
    @GetMapping("/tickets")
    public List<AgentSaleResponse> mySales(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return ticketService.agentSales(user.userId());
    }

    @Operation(summary = "Get one of my sales")
    @GetMapping("/tickets/{id}")
    public AgentSaleResponse mySale(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        return ticketService.agentSale(user.userId(), id);
    }

    @Operation(summary = "Cancel my sale",
            description = "Refunds the ticket and reverses the commission")
    @PatchMapping("/tickets/{id}/cancel")
    public AgentSaleResponse cancel(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ticketService.cancelByAgent(user.userId(), authHeader, id);
    }

    @Operation(summary = "My commissions",
            description = "Totals earned and reversed, with each line")
    @GetMapping("/commissions")
    public CommissionSummary commissions(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return ticketService.myCommissions(user.userId());
    }
}