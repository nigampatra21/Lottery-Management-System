package com.lottery.ticket_service.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentReconciliationJob {

    private final TicketService ticketService;

    public PaymentReconciliationJob(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void run() {
        ticketService.reconcilePending();
    }
}