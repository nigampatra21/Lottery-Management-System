package com.lottery.ticket_service.service;

import com.lottery.ticket_service.exception.ServiceUnavailableException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RazorpayServiceTest {

    @Test
    void missingCredentialsDoNotPreventServiceCreationButRejectOrderRequests() {
        RazorpayService service = new RazorpayService("", "");

        assertThrows(ServiceUnavailableException.class,
                () -> service.createOrder(BigDecimal.ONE, "TEST-RECEIPT"));
    }
}
