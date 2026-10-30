
package com.lottery.ticket_service.dto;

import java.math.BigDecimal;

public record PaymentOrderResponse(
        String ticketId,
        String orderId,
        BigDecimal amount,
        long amountPaise,
        String currency,
        String razorpayKeyId,
        String status
) {}
