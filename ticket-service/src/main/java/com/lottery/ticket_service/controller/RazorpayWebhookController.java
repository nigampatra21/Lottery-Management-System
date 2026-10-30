package com.lottery.ticket_service.controller;

import com.lottery.ticket_service.service.TicketService;
import com.lottery.ticket_service.service.WebhookSignatureVerifier;
import io.swagger.v3.oas.annotations.Hidden;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Hidden
@RestController
@RequestMapping("/api/webhooks")
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final WebhookSignatureVerifier verifier;
    private final TicketService ticketService;

    public RazorpayWebhookController(WebhookSignatureVerifier verifier,
                                     TicketService ticketService) {
        this.verifier = verifier;
        this.ticketService = ticketService;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<Void> razorpay(
            @RequestBody String body,                      // the raw body is what was signed
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        if (!verifier.isConfigured()) {
            return ResponseEntity.status(503).build();
        }
        if (!verifier.isValid(body, signature)) {
            log.warn("Razorpay webhook rejected: invalid signature");
            return ResponseEntity.status(401).build();
        }

        try {
            JSONObject json = new JSONObject(body);
            if ("payment.captured".equals(json.optString("event"))) {
                JSONObject payment = json.getJSONObject("payload")
                        .getJSONObject("payment").getJSONObject("entity");
                String orderId = payment.optString("order_id", "");
                String paymentId = payment.optString("id", "");
                if (!orderId.isBlank() && !paymentId.isBlank()) {
                    ticketService.handleCapturedPayment(orderId, paymentId);
                }
            }
        } catch (RuntimeException e) {
            // A 500 makes Razorpay retry later, which is what we want for a temporary failure
            log.warn("Razorpay webhook could not be processed: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok().build();
    }
}