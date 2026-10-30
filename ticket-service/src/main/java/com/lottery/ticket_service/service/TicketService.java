package com.lottery.ticket_service.service;

import com.lottery.ticket_service.audit.AuditClient;
import com.lottery.ticket_service.client.DrawClient;
import com.lottery.ticket_service.client.LotteryClient;
import com.lottery.ticket_service.dto.*;
import com.lottery.ticket_service.entity.*;
import com.lottery.ticket_service.exception.BusinessRuleException;
import com.lottery.ticket_service.exception.DuplicateResourceException;
import com.lottery.ticket_service.exception.ResourceNotFoundException;
import com.lottery.ticket_service.repository.AgentCommissionRepository;
import com.lottery.ticket_service.repository.TicketRepository;
import com.lottery.ticket_service.repository.TicketTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    // Status in lottery-service that means "open for sales"
    private static final String OPEN_STATUS = "ACTIVE";
    // Zone in which the lottery's sales and draw dates are entered
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int MAX_ID_ATTEMPTS = 5;
    private static final int MAX_PENDING_PER_CUSTOMER = 5;
    // Statuses in which a ticket holds its numbers
    private static final List<TicketStatus> HOLDS_NUMBERS =
            List.of(TicketStatus.PENDING_PAYMENT, TicketStatus.ISSUED);

    private final TicketRepository ticketRepository;
    private final TicketTransactionRepository transactionRepository;
    private final AgentCommissionRepository commissionRepository;
    private final LotteryClient lotteryClient;
    private final DrawClient drawClient;
    private final TicketCodeGenerator codeGenerator;
    private final TransactionTemplate tx;
    private final AuditClient auditClient;
    private final RazorpayService razorpayService;
    private final long reconcileAfterMinutes;
    private final long expireAfterMinutes;

    public TicketService(TicketRepository ticketRepository,
                         TicketTransactionRepository transactionRepository,
                         AgentCommissionRepository commissionRepository,
                         LotteryClient lotteryClient,
                         DrawClient drawClient,
                         TicketCodeGenerator codeGenerator,
                         TransactionTemplate tx,
                         AuditClient auditClient,
                         RazorpayService razorpayService,
                         @Value("${payments.reconcile-after-minutes:2}") long reconcileAfterMinutes,
                         @Value("${payments.expire-after-minutes:15}") long expireAfterMinutes) {
        this.ticketRepository = ticketRepository;
        this.transactionRepository = transactionRepository;
        this.commissionRepository = commissionRepository;
        this.lotteryClient = lotteryClient;
        this.drawClient = drawClient;
        this.codeGenerator = codeGenerator;
        this.tx = tx;
        this.auditClient = auditClient;
        this.razorpayService = razorpayService;
        this.reconcileAfterMinutes = reconcileAfterMinutes;
        this.expireAfterMinutes = expireAfterMinutes;
    }

    private record Prepared(LotteryPurchaseInfo info, String ticketNumber) {}

    private record IssuedSale(TicketResponse ticket, BigDecimal rate, BigDecimal commission) {}

    private record CancelResult(TicketResponse ticket, Long refundTransactionId,
                                boolean gatewayRefund) {}

    // ---------- shared checks for any new ticket ----------

    private Prepared prepare(Long lotteryId, List<Integer> selected, String authHeader) {
        // Remote calls first, outside any database transaction
        LotteryPurchaseInfo info = lotteryClient.getPurchaseInfo(lotteryId, authHeader);

        if (!OPEN_STATUS.equals(info.status())) {
            throw new BusinessRuleException("Ticket sales are not open for this lottery");
        }
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        if (now.isBefore(info.salesStartAt())) {
            throw new BusinessRuleException("Ticket sales have not started yet");
        }
        if (now.isAfter(info.salesEndAt())) {
            throw new BusinessRuleException("Ticket sales have ended");
        }
        // The winning numbers are known once a draw exists, so selling must stop
        drawClient.requireNoDraw(info.lotteryId(), authHeader,
                "Ticket sales are closed because the draw has been held");

        List<Integer> numbers = validateNumbers(selected, info);
        String ticketNumber = numbers.stream()
                .map(n -> String.format("%02d", n))
                .collect(Collectors.joining("-"));

        if (ticketRepository.existsByLotteryIdAndTicketNumberAndStatusIn(
                info.lotteryId(), ticketNumber, HOLDS_NUMBERS)) {
            throw new DuplicateResourceException(
                    "These numbers have already been sold for this lottery");
        }
        // Unpaid tickets hold a place too, so they count against the limit
        if (info.maxTickets() != null
                && ticketRepository.countByLotteryIdAndStatusIn(
                info.lotteryId(), HOLDS_NUMBERS) >= info.maxTickets()) {
            throw new BusinessRuleException("All tickets for this lottery have been sold");
        }
        return new Prepared(info, ticketNumber);
    }

    // ---------- customer purchase (online payment through Razorpay) ----------

    public PaymentOrderResponse purchase(Long customerId, String authHeader,
                                         TicketPurchaseRequest req) {
        Prepared prepared = prepare(req.lotteryId(), req.selectedNumbers(), authHeader);
        LotteryPurchaseInfo info = prepared.info();

        if (!"INR".equalsIgnoreCase(info.currency())) {
            throw new BusinessRuleException("Online payment supports INR lotteries only");
        }
        if (ticketRepository.countByCustomerIdAndStatus(
                customerId, TicketStatus.PENDING_PAYMENT) >= MAX_PENDING_PER_CUSTOMER) {
            throw new BusinessRuleException(
                    "You have too many unpaid tickets. Complete them or wait for them to expire");
        }

        Ticket ticket = new Ticket();
        ticket.setTicketId(uniqueTicketId());
        ticket.setLotteryId(info.lotteryId());
        ticket.setCustomerId(customerId);
        ticket.setTicketNumber(prepared.ticketNumber());
        ticket.setPrice(info.ticketPrice());
        ticket.setStatus(TicketStatus.PENDING_PAYMENT);
        ticket.setPurchasedAt(LocalDateTime.now(ZoneOffset.UTC));

        final Ticket saved = createPendingTicket(ticket);

        final String orderId;
        try {
            orderId = razorpayService.createOrder(saved.getPrice(), saved.getTicketId());
        } catch (RuntimeException e) {
            // Release the numbers instead of leaving an orphaned unpaid ticket
            tx.executeWithoutResult(s ->
                    ticketRepository.findByIdForUpdate(saved.getId())
                            .ifPresent(this::expireTicket));
            throw e;
        }

        tx.executeWithoutResult(status -> {
            TicketTransaction transaction = transactionRepository
                    .findFirstByTicketIdAndTransactionTypeAndPaymentMethodOrderByIdAsc(
                            saved.getId(), TransactionType.PURCHASE, PaymentMethod.RAZORPAY)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Purchase transaction not found"));
            transaction.setGatewayOrderId(orderId);
            transactionRepository.save(transaction);
        });

        return new PaymentOrderResponse(
                saved.getTicketId(),
                orderId,
                saved.getPrice(),
                saved.getPrice().multiply(new BigDecimal("100")).longValueExact(),
                "INR",
                razorpayService.getKeyId(),
                "PENDING");
    }

    private Ticket createPendingTicket(Ticket ticket) {
        try {
            Ticket created = tx.execute(status -> {
                Ticket savedTicket = ticketRepository.save(ticket);
                transactionRepository.save(newTransaction(savedTicket.getId(),
                        TransactionType.PURCHASE, savedTicket.getPrice(),
                        PaymentMethod.RAZORPAY));
                return savedTicket;
            });
            if (created == null) {
                throw new IllegalStateException("Ticket could not be saved");
            }
            return created;
        } catch (DataIntegrityViolationException e) {
            log.warn("Ticket insert rejected: {}", e.getMostSpecificCause().getMessage());
            if (isNumberConflict(e)) {
                throw new DuplicateResourceException(
                        "These numbers have already been sold for this lottery");
            }
            throw e;
        }
    }

    // ---------- payment verification ----------

    public PaymentVerificationResponse verifyPaymentAndIssueTicket(
            Long customerId, String orderId, String paymentId) {

        TicketTransaction transaction = transactionRepository
                .findByGatewayOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));

        Ticket ticket = ticketRepository
                .findByIdAndCustomerId(transaction.getTicketId(), customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (transaction.getTransactionType() != TransactionType.PURCHASE
                || transaction.getPaymentMethod() != PaymentMethod.RAZORPAY) {
            throw new BusinessRuleException("Invalid purchase transaction");
        }

        if (transaction.getStatus() == TransactionStatus.SUCCESS
                && ticket.getStatus() == TicketStatus.ISSUED
                && paymentId.equals(transaction.getExternalReference())) {
            return new PaymentVerificationResponse(ticket.getTicketId(), paymentId, "SUCCESS",
                    "Payment was already verified and the ticket is issued");
        }

        boolean expired = ticket.getStatus() == TicketStatus.EXPIRED;
        if (!expired && (ticket.getStatus() != TicketStatus.PENDING_PAYMENT
                || transaction.getStatus() != TransactionStatus.PENDING)) {
            throw new BusinessRuleException("Ticket is not awaiting payment");
        }

        if (!razorpayService.isPaymentCapturedForOrder(
                paymentId, orderId, transaction.getAmount())) {
            throw new BusinessRuleException(
                    "Payment has not been captured or payment details do not match");
        }

        if (expired) {
            // The customer paid after the ticket expired: give the money back
            refundLatePayment(ticket, orderId, paymentId);
            return new PaymentVerificationResponse(ticket.getTicketId(), paymentId, "REFUNDED",
                    "The ticket had expired, so the payment is being refunded");
        }

        if (!markIssued(ticket.getId(), orderId, paymentId)) {
            throw new BusinessRuleException("Ticket payment has already been processed");
        }
        return new PaymentVerificationResponse(ticket.getTicketId(), paymentId, "SUCCESS",
                "Payment verified and ticket issued successfully");
    }

    /** Called by the Razorpay webhook. Safe to call more than once for the same payment. */
    public void handleCapturedPayment(String orderId, String paymentId) {
        TicketTransaction transaction =
                transactionRepository.findByGatewayOrderId(orderId).orElse(null);
        if (transaction == null) {
            log.info("Webhook for unknown order {} ignored", orderId);
            return;
        }
        Ticket ticket = ticketRepository.findById(transaction.getTicketId()).orElse(null);
        if (ticket == null) {
            return;
        }
        // Already issued, cancelled or refunded: nothing to do
        if (ticket.getStatus() != TicketStatus.PENDING_PAYMENT
                && ticket.getStatus() != TicketStatus.EXPIRED) {
            return;
        }
        // Trust, but check with Razorpay: order, amount and currency must match
        if (!razorpayService.isPaymentCapturedForOrder(
                paymentId, orderId, transaction.getAmount())) {
            log.warn("Webhook payment {} does not match order {}; ignored", paymentId, orderId);
            return;
        }
        if (ticket.getStatus() == TicketStatus.EXPIRED) {
            // The customer paid after the ticket expired: give the money back
            refundLatePayment(ticket, orderId, paymentId);
            log.info("Late payment {} for expired ticket {} is being refunded",
                    paymentId, ticket.getTicketId());
        } else if (markIssued(ticket.getId(), orderId, paymentId)) {
            log.info("Ticket {} issued by webhook (payment {})", ticket.getTicketId(), paymentId);
        }
    }

    /** Pending to issued, atomically. Returns false if it was already processed. */
    private boolean markIssued(Long ticketId, String orderId, String paymentId) {
        Boolean done = tx.execute(status -> {
            Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
            TicketTransaction transaction = transactionRepository
                    .findByGatewayOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));

            if (ticket.getStatus() != TicketStatus.PENDING_PAYMENT
                    || transaction.getStatus() != TransactionStatus.PENDING) {
                return false;
            }
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setExternalReference(paymentId);
            ticket.setStatus(TicketStatus.ISSUED);
            ticket.setIssuedAt(LocalDateTime.now(ZoneOffset.UTC));
            transactionRepository.save(transaction);
            ticketRepository.save(ticket);
            return true;
        });
        return Boolean.TRUE.equals(done);
    }

    private void refundLatePayment(Ticket ticket, String orderId, String paymentId) {
        Long refundTransactionId = tx.execute(status -> {
            if (transactionRepository.findFirstByTicketIdAndTransactionTypeOrderByIdAsc(
                    ticket.getId(), TransactionType.REFUND).isPresent()) {
                return null;                              // already being refunded
            }
            TicketTransaction purchase = transactionRepository.findByGatewayOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
            // The money really was captured, so the ledger must say so
            purchase.setStatus(TransactionStatus.SUCCESS);
            purchase.setExternalReference(paymentId);
            transactionRepository.save(purchase);
            return transactionRepository.save(newTransaction(ticket.getId(),
                    TransactionType.REFUND, ticket.getPrice(), PaymentMethod.RAZORPAY)).getId();
        });
        if (refundTransactionId != null) {
            tryRefund(refundTransactionId);
        }
    }

    // ---------- agent sales (payment is taken by the agent, offline) ----------

    public AgentSaleResponse sell(Long agentId, String authHeader, AgentSaleRequest req) {
        if (req.paymentMethod() == PaymentMethod.RAZORPAY) {
            throw new BusinessRuleException(
                    "Agent sales are paid offline: use cash, card, UPI, net banking or wallet");
        }
        if (agentId.equals(req.customerId())) {
            throw new BusinessRuleException("An agent cannot sell a ticket to themselves");
        }

        Prepared prepared = prepare(req.lotteryId(), req.selectedNumbers(), authHeader);
        LotteryPurchaseInfo info = prepared.info();

        final BigDecimal rate = info.agentCommissionPercent() == null
                ? BigDecimal.ZERO : info.agentCommissionPercent();

        LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
        Ticket ticket = new Ticket();
        ticket.setTicketId(uniqueTicketId());
        ticket.setLotteryId(info.lotteryId());
        ticket.setCustomerId(req.customerId());          // null means a walk-in customer
        ticket.setAgentId(agentId);
        ticket.setTicketNumber(prepared.ticketNumber());
        ticket.setPrice(info.ticketPrice());
        ticket.setStatus(TicketStatus.ISSUED);
        ticket.setPurchasedAt(utcNow);
        ticket.setIssuedAt(utcNow);

        IssuedSale sale;
        try {
            // Ticket, payment record, commission and audit record: all or nothing
            sale = tx.execute(status -> {
                Ticket saved = ticketRepository.save(ticket);

                TicketTransaction payment = newTransaction(saved.getId(),
                        TransactionType.PURCHASE, saved.getPrice(), req.paymentMethod());
                payment.setStatus(TransactionStatus.SUCCESS);   // the agent already took the money
                transactionRepository.save(payment);

                // The server calculates the commission: price x rate / 100
                BigDecimal amount = saved.getPrice().multiply(rate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                AgentCommission c = new AgentCommission();
                c.setTicketId(saved.getId());
                c.setAgentId(agentId);
                c.setRatePercent(rate);
                c.setTicketPrice(saved.getPrice());
                c.setCommissionAmount(amount);
                c.setStatus(CommissionStatus.EARNED);
                c.setEarnedAt(utcNow);
                commissionRepository.save(c);

                Map<String, Object> details = new LinkedHashMap<>();
                details.put("lotteryId", info.lotteryId());
                details.put("customerId", req.customerId());     // null means walk-in
                details.put("price", saved.getPrice().toPlainString());
                details.put("paymentMethod", req.paymentMethod().name());
                details.put("commissionRate", rate.toPlainString());
                details.put("commissionAmount", amount.toPlainString());
                auditClient.record(agentId, "AGENT_TICKET_SOLD", "TICKET",
                        String.valueOf(saved.getId()), null, details);

                return new IssuedSale(TicketResponse.from(saved), rate, amount);
            });
        } catch (DataIntegrityViolationException e) {
            log.warn("Ticket insert rejected: {}", e.getMostSpecificCause().getMessage());
            if (isNumberConflict(e)) {
                throw new DuplicateResourceException(
                        "These numbers have already been sold for this lottery");
            }
            throw e;
        }

        return new AgentSaleResponse(sale.ticket(), req.customerId(),
                sale.rate(), sale.commission(), CommissionStatus.EARNED.name());
    }

    public List<AgentSaleResponse> agentSales(Long agentId) {
        List<Ticket> tickets = ticketRepository.findByAgentIdOrderByPurchasedAtDesc(agentId);
        if (tickets.isEmpty()) {
            return List.of();
        }
        Map<Long, AgentCommission> byTicket = commissionRepository
                .findByTicketIdIn(tickets.stream().map(Ticket::getId).toList())
                .stream().collect(Collectors.toMap(AgentCommission::getTicketId,
                        Function.identity()));
        return tickets.stream().map(t -> toAgentSale(t, byTicket.get(t.getId()))).toList();
    }

    public AgentSaleResponse agentSale(Long agentId, Long id) {
        Ticket ticket = findOwnedByAgent(agentId, id);
        return toAgentSale(ticket, commissionRepository.findByTicketId(id).orElse(null));
    }

    public AgentSaleResponse cancelByAgent(Long agentId, String authHeader, Long id) {
        findOwnedByAgent(agentId, id);                   // 404 if it isn't their sale
        cancelTicket(id, agentId, authHeader);
        return agentSale(agentId, id);
    }

    public CommissionSummary myCommissions(Long agentId) {
        List<AgentCommission> all = commissionRepository.findByAgentIdOrderByIdDesc(agentId);
        return new CommissionSummary(sum(all, CommissionStatus.EARNED),
                sum(all, CommissionStatus.REVERSED),
                all.stream().map(CommissionResponse::from).toList());
    }

    // ---------- customer reads ----------

    public List<TicketResponse> listMine(Long customerId) {
        return ticketRepository.findByCustomerIdOrderByPurchasedAtDesc(customerId)
                .stream().map(TicketResponse::from).toList();
    }

    public TicketResponse getMine(Long customerId, Long id) {
        return TicketResponse.from(findOwned(customerId, id));
    }

    public List<TransactionResponse> transactionsFor(Long customerId, Long ticketId) {
        Ticket ticket = findOwned(customerId, ticketId);
        return transactionRepository.findByTicketIdOrderByIdAsc(ticket.getId())
                .stream().map(TransactionResponse::from).toList();
    }

    // ---------- cancellation (customer and agent) ----------

    public TicketResponse cancel(Long customerId, String authHeader, Long id) {
        Ticket ticket = findOwned(customerId, id);       // 404 if it isn't theirs
        if (ticket.getAgentId() != null) {
            // The customer paid the agent, so only the agent can hand the money back
            throw new BusinessRuleException(
                    "This ticket was sold by an agent. Ask your agent to cancel it");
        }
        return cancelTicket(id, customerId, authHeader).ticket();
    }

    /** The caller has already checked who owns the ticket. */
    private CancelResult cancelTicket(Long id, Long actorId, String authHeader) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        if (ticket.getStatus() != TicketStatus.ISSUED) {
            throw new BusinessRuleException("Only issued tickets can be cancelled");
        }
        LotteryPurchaseInfo info =
                lotteryClient.getPurchaseInfo(ticket.getLotteryId(), authHeader);
        if (!info.allowTicketCancellation()) {
            throw new BusinessRuleException(
                    "Ticket cancellation is not allowed for this lottery");
        }
        LocalDateTime deadline = info.cancellationCutoffMinutes() == null
                ? info.drawAt()
                : info.drawAt().minusMinutes(info.cancellationCutoffMinutes());
        if (LocalDateTime.now(BUSINESS_ZONE).isAfter(deadline)) {
            throw new BusinessRuleException("The cancellation window has closed");
        }
        // A refund after the draw would be unfair, because the winning numbers are known
        drawClient.requireNoDraw(ticket.getLotteryId(), authHeader,
                "Tickets cannot be cancelled after the draw has been held");

        CancelResult result = tx.execute(status -> {
            // Re-read with a lock so two cancel requests can't both refund
            Ticket locked = ticketRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
            if (locked.getStatus() != TicketStatus.ISSUED) {
                throw new BusinessRuleException("Only issued tickets can be cancelled");
            }

            LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
            locked.setStatus(TicketStatus.CANCELLED);
            locked.setCancelledAt(utcNow);
            ticketRepository.save(locked);

            // Refund through the same method that was used to pay
            PaymentMethod method = transactionRepository
                    .findFirstByTicketIdAndTransactionTypeOrderByIdAsc(
                            locked.getId(), TransactionType.PURCHASE)
                    .map(TicketTransaction::getPaymentMethod)
                    .orElse(null);
            boolean gateway = method == PaymentMethod.RAZORPAY;

            TicketTransaction refund = newTransaction(locked.getId(),
                    TransactionType.REFUND, locked.getPrice(), method);
            // A gateway refund completes later. An offline refund is done at once.
            refund.setStatus(gateway ? TransactionStatus.PENDING : TransactionStatus.SUCCESS);
            Long refundId = transactionRepository.save(refund).getId();

            // An agent's commission on a cancelled ticket is reversed
            BigDecimal reversed = BigDecimal.ZERO;
            AgentCommission commission =
                    commissionRepository.findByTicketId(locked.getId()).orElse(null);
            if (commission != null && commission.getStatus() == CommissionStatus.EARNED) {
                commission.setStatus(CommissionStatus.REVERSED);
                commission.setReversedAt(utcNow);
                commissionRepository.save(commission);
                reversed = commission.getCommissionAmount();
            }

            // Inside the transaction, so the cancellation and its audit record cannot separate
            auditClient.record(actorId, "TICKET_CANCELLED", "TICKET", String.valueOf(id),
                    Map.of("status", "ISSUED"),
                    Map.of("status", "CANCELLED",
                            "lotteryId", locked.getLotteryId(),
                            "refundAmount", locked.getPrice().toPlainString(),
                            "commissionReversed", reversed.toPlainString()));

            return new CancelResult(TicketResponse.from(locked), refundId, gateway);
        });

        if (result.gatewayRefund()) {
            tryRefund(result.refundTransactionId());
        }
        return result;
    }

    /** Asks Razorpay to refund. If it fails, the refund stays PENDING and the job retries. */
    private void tryRefund(Long refundTransactionId) {
        try {
            TicketTransaction refund =
                    transactionRepository.findById(refundTransactionId).orElse(null);
            if (refund == null || refund.getStatus() != TransactionStatus.PENDING
                    || refund.getPaymentMethod() != PaymentMethod.RAZORPAY) {
                return;
            }
            TicketTransaction purchase = transactionRepository
                    .findFirstByTicketIdAndTransactionTypeAndPaymentMethodOrderByIdAsc(
                            refund.getTicketId(), TransactionType.PURCHASE,
                            PaymentMethod.RAZORPAY)
                    .orElse(null);
            if (purchase == null || purchase.getExternalReference() == null) {
                return;
            }
            String refundId = razorpayService.refundPayment(
                    purchase.getExternalReference(), refund.getAmount());
            tx.executeWithoutResult(status -> {
                refund.setStatus(TransactionStatus.SUCCESS);
                refund.setExternalReference(refundId);
                transactionRepository.save(refund);
            });
        } catch (RuntimeException e) {
            log.warn("Refund for transaction {} is not complete yet: {}",
                    refundTransactionId, e.getMessage());
        }
    }

    // ---------- background reconciliation (called by PaymentReconciliationJob) ----------

    public void reconcilePending() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<Ticket> pending = ticketRepository
                .findTop50ByStatusAndPurchasedAtBeforeOrderByIdAsc(
                        TicketStatus.PENDING_PAYMENT, now.minusMinutes(reconcileAfterMinutes));
        for (Ticket ticket : pending) {
            try {
                reconcileOne(ticket, now);
            } catch (RuntimeException e) {
                log.warn("Could not reconcile ticket {}: {}", ticket.getTicketId(), e.getMessage());
            }
        }

        // Retry gateway refunds that did not complete the first time
        for (TicketTransaction refund : transactionRepository
                .findTop50ByTransactionTypeAndStatusAndPaymentMethodOrderByIdAsc(
                        TransactionType.REFUND, TransactionStatus.PENDING,
                        PaymentMethod.RAZORPAY)) {
            tryRefund(refund.getId());
        }
    }

    private void reconcileOne(Ticket ticket, LocalDateTime now) {
        TicketTransaction purchase = transactionRepository
                .findFirstByTicketIdAndTransactionTypeAndPaymentMethodOrderByIdAsc(
                        ticket.getId(), TransactionType.PURCHASE, PaymentMethod.RAZORPAY)
                .orElse(null);
        String orderId = purchase == null ? null : purchase.getGatewayOrderId();

        // The customer may have paid and never come back to verify
        if (orderId != null) {
            for (String paymentId : razorpayService.findCapturedPaymentIds(orderId)) {
                if (razorpayService.isPaymentCapturedForOrder(
                        paymentId, orderId, purchase.getAmount())) {
                    if (markIssued(ticket.getId(), orderId, paymentId)) {
                        log.info("Ticket {} issued by reconciliation (payment {})",
                                ticket.getTicketId(), paymentId);
                    }
                    return;
                }
            }
        }

        if (ticket.getPurchasedAt().isBefore(now.minusMinutes(expireAfterMinutes))) {
            tx.executeWithoutResult(s ->
                    ticketRepository.findByIdForUpdate(ticket.getId())
                            .ifPresent(this::expireTicket));
            log.info("Unpaid ticket {} expired", ticket.getTicketId());
        }
    }

    // Call inside a transaction, on a ticket loaded with a lock
    private void expireTicket(Ticket ticket) {
        if (ticket.getStatus() != TicketStatus.PENDING_PAYMENT) {
            return;
        }
        ticket.setStatus(TicketStatus.EXPIRED);
        ticketRepository.save(ticket);
        for (TicketTransaction t : transactionRepository.findByTicketIdOrderByIdAsc(ticket.getId())) {
            if (t.getStatus() == TransactionStatus.PENDING) {
                t.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(t);
            }
        }
    }

    // ---------- admin (used by winner-service) ----------

    public AdminTicketPage issuedTicketsForLottery(Long lotteryId, Long afterId, int limit) {
        int size = Math.min(Math.max(limit, 1), 1000);
        List<Ticket> rows = ticketRepository
                .findByLotteryIdAndStatusAndIdGreaterThanOrderByIdAsc(
                        lotteryId, TicketStatus.ISSUED, afterId, Limit.of(size));
        List<AdminTicketView> views = rows.stream().map(AdminTicketView::from).toList();
        // A full page means there may be more, so hand back the cursor
        Long next = rows.size() == size ? rows.get(rows.size() - 1).getId() : null;
        return new AdminTicketPage(views, next);
    }

    // ---------- helpers ----------

    private AgentSaleResponse toAgentSale(Ticket t, AgentCommission c) {
        return new AgentSaleResponse(TicketResponse.from(t), t.getCustomerId(),
                c == null ? null : c.getRatePercent(),
                c == null ? null : c.getCommissionAmount(),
                c == null ? null : c.getStatus().name());
    }

    private BigDecimal sum(List<AgentCommission> list, CommissionStatus status) {
        return list.stream().filter(c -> c.getStatus() == status)
                .map(AgentCommission::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private TicketTransaction newTransaction(Long ticketId, TransactionType type,
                                             BigDecimal amount, PaymentMethod method) {
        TicketTransaction t = new TicketTransaction();
        t.setTicketId(ticketId);
        t.setTransactionReference(codeGenerator.next("TXN-"));
        t.setTransactionType(type);
        t.setAmount(amount);
        t.setStatus(TransactionStatus.PENDING);
        t.setPaymentMethod(method);
        return t;
    }

    // Only a clash on the numbers key means "already sold". Anything else is a real error.
    private boolean isNumberConflict(DataIntegrityViolationException e) {
        String message = String.valueOf(e.getMostSpecificCause().getMessage());
        return message.contains("Duplicate entry")
                && message.contains("uq_lottery_active_number");
    }

    // 404 (not 403) for other people's tickets, so ticket ids can't be probed
    private Ticket findOwned(Long customerId, Long id) {
        return ticketRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private Ticket findOwnedByAgent(Long agentId, Long id) {
        return ticketRepository.findByIdAndAgentId(id, agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private List<Integer> validateNumbers(List<Integer> input, LotteryPurchaseInfo info) {
        if (input.size() != info.numberCount()) {
            throw new BusinessRuleException(
                    "Select exactly " + info.numberCount() + " numbers");
        }
        Set<Integer> seen = new HashSet<>();
        for (Integer n : input) {
            if (n < info.minNumber() || n > info.maxNumber()) {
                throw new BusinessRuleException("Numbers must be between "
                        + info.minNumber() + " and " + info.maxNumber());
            }
            if (!seen.add(n)) {
                throw new BusinessRuleException("Numbers must not repeat");
            }
        }
        return input.stream().sorted().toList();
    }

    private String uniqueTicketId() {
        for (int i = 0; i < MAX_ID_ATTEMPTS; i++) {
            String id = codeGenerator.next("TKT-");
            if (!ticketRepository.existsByTicketId(id)) {
                return id;
            }
        }
        throw new IllegalStateException("Could not generate a unique ticket id");
    }
}