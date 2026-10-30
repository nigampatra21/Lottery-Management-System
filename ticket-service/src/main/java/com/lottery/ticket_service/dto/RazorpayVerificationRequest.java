package com.lottery.ticket_service.dto;

import jakarta.validation.constraints.NotBlank;

public record RazorpayVerificationRequest(

        @NotBlank
        String razorpay_order_id,

        @NotBlank
        String razorpay_payment_id,

        @NotBlank
        String razorpay_signature

) {}