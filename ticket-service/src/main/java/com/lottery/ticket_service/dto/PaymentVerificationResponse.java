package com.lottery.ticket_service.dto;

public record PaymentVerificationResponse(
        String ticketId,
        String paymentId,
        String status,
        String message
) {}