package com.lottery.ticket_service.controller;

import com.lottery.ticket_service.dto.*;
import com.lottery.ticket_service.exception.BusinessRuleException;
import com.lottery.ticket_service.security.AuthenticatedUser;
import com.lottery.ticket_service.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.lottery.ticket_service.service.RazorpaySignatureService;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets (customer)", description = "Buy and manage your own tickets. Role: CUSTOMER")
public class TicketController {

    private final TicketService ticketService;
    private final RazorpaySignatureService razorpaySignatureService;
    public TicketController(TicketService ticketService, RazorpaySignatureService razorpaySignatureService) {
        this.ticketService = ticketService;
        this.razorpaySignatureService = razorpaySignatureService;
    }

    @Operation(summary = "Buy a ticket",
            description = "Price, status and ticket id are set by the server")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentOrderResponse purchase(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody TicketPurchaseRequest req) {
        return ticketService.purchase(user.userId(), authHeader, req);
    }

    @Operation(summary = "List my tickets")
    @GetMapping("/my")
    public List<TicketResponse> myTickets(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return ticketService.listMine(user.userId());
    }

    @Operation(summary = "Get one of my tickets",
            description = "Another customer's ticket returns 404")
    @GetMapping("/my/{id}")
    public TicketResponse myTicket(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        return ticketService.getMine(user.userId(), id);
    }

    @Operation(summary = "Payment history of my ticket")
    @GetMapping("/my/{id}/transactions")
    public List<TransactionResponse> transactions(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        return ticketService.transactionsFor(user.userId(), id);
    }

    @Operation(summary = "Cancel my ticket and refund",
            description = "Only if the lottery allows cancellation and the window is open")
    @PatchMapping("/my/{id}/cancel")
    public TicketResponse cancel(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ticketService.cancel(user.userId(), authHeader, id);
    }
    @Operation(
            summary = "Verify Razorpay payment and issue ticket",
            description = "Verifies payment signature and gateway status"
    )
    @PostMapping("/verify-payment")
    public ResponseEntity<PaymentVerificationResponse>
    verifyPaymentAndIssueTicket(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, String> request
    ) {
        String orderId = request.get("razorpay_order_id");
        String paymentId = request.get("razorpay_payment_id");
        String signature = request.get("razorpay_signature");

        if (orderId == null || paymentId == null || signature == null
                || orderId.isBlank() || paymentId.isBlank()
                || signature.isBlank()) {
            throw new BusinessRuleException(
                    "Razorpay order ID, payment ID and signature are required"
            );
        }

        boolean validSignature =
                razorpaySignatureService.verifyPaymentSignature(
                        orderId,
                        paymentId,
                        signature
                );

        if (!validSignature) {
            throw new BusinessRuleException(
                    "Invalid Razorpay payment signature"
            );
        }

        PaymentVerificationResponse response =
                ticketService.verifyPaymentAndIssueTicket(
                        user.userId(),
                        orderId,
                        paymentId
                );

        return ResponseEntity.ok(response);
    }

}