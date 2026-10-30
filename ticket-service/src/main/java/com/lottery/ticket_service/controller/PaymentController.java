
package com.lottery.ticket_service.controller;

import com.lottery.ticket_service.service.RazorpayService;
import com.lottery.ticket_service.exception.ServiceUnavailableException;
import com.lottery.ticket_service.service.RazorpaySignatureService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final RazorpayService razorpayService;
    private final RazorpaySignatureService signatureService;


    public PaymentController(
            RazorpayService razorpayService,
            RazorpaySignatureService signatureService) {
        this.razorpayService = razorpayService;
        this.signatureService = signatureService;
    }


    @PostMapping("/test-order")
    public ResponseEntity<?> createTestOrder(
            @RequestBody Map<String, BigDecimal> request) {

        try {
            BigDecimal amount = request.get("amount");

            String orderId = razorpayService.createOrder(
                    amount, "TEST-" + System.currentTimeMillis());

            return ResponseEntity.ok(Map.of(
                    "orderId", orderId,
                    "amount", amount,
                    "currency", "INR",
                    "message", "Razorpay order created successfully"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage()));

        } catch (ServiceUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    Map.of("error", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                    Map.of("error", "Could not create Razorpay order"));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestBody Map<String, String> request) {

        String orderId = request.get("razorpay_order_id");
        String paymentId = request.get("razorpay_payment_id");
        String signature = request.get("razorpay_signature");

        if (orderId == null || paymentId == null || signature == null
                || orderId.isBlank() || paymentId.isBlank()
                || signature.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Missing payment verification fields"));
        }

        boolean verified = signatureService.verifyPaymentSignature(
                orderId, paymentId, signature);

        if (!verified) {
            return ResponseEntity.status(400).body(
                    Map.of("verified", false,
                            "message", "Invalid payment signature"));
        }

        return ResponseEntity.ok(Map.of(
                "verified", true,
                "orderId", orderId,
                "paymentId", paymentId,
                "message", "Payment signature verified"
        ));
    }

}
