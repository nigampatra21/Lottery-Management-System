package com.lottery.winner.service;

import com.lottery.winner.audit.AuditClient;
import com.lottery.winner.client.ServiceClients;
import com.lottery.winner.dto.*;
import com.lottery.winner.entity.Winner;
import com.lottery.winner.exception.BusinessRuleException;
import com.lottery.winner.exception.DuplicateResourceException;
import com.lottery.winner.repository.WinnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class WinnerService {

    private static final Logger log = LoggerFactory.getLogger(WinnerService.class);
    private static final int MAX_ID_ATTEMPTS = 5;
    private final WinnerRepository winnerRepository;
    private final ServiceClients clients;
    private final TransactionTemplate tx;
    private final CodeGenerator codeGenerator;
    private final AuditClient auditClient;
    private final SecureRandom random = new SecureRandom();

    public WinnerService(WinnerRepository winnerRepository,
                         ServiceClients clients, TransactionTemplate tx, CodeGenerator codeGenerator, AuditClient auditClient) {
        this.winnerRepository = winnerRepository;
        this.clients = clients;
        this.tx = tx;
        this.codeGenerator = codeGenerator;
        this.auditClient = auditClient;
    }

    public DeclareResult declare(Long adminUserId, String auth, DeclareRequest req) {
        DrawView draw = clients.getDraw(req.drawId(), auth);
        if (!"COMPLETED".equals(draw.status())) {
            throw new BusinessRuleException("The draw has not been completed");
        }
        if (winnerRepository.existsByDrawId(draw.id())) {
            throw new DuplicateResourceException(
                    "Winners have already been declared for this draw");
        }

        List<PrizeTierView> tiers = clients.getPrizeTiers(draw.lotteryId(), auth);
        if (tiers.isEmpty()) {
            throw new BusinessRuleException("This lottery has no prize tiers");
        }
        Map<Integer, PrizeTierView> tierByMatch = new HashMap<>();
        for (PrizeTierView t : tiers) {
            tierByMatch.put(t.matchCount(), t);
        }

        // Read every issued ticket page by page and group the ones that hit a tier
        Set<Integer> winning = new HashSet<>(draw.winningNumbers());
        Map<Integer, List<AdminTicketView>> hits = new HashMap<>();
        long scanned = 0;
        Long next = 0L;
        while (next != null) {
            AdminTicketPage page = clients.getTickets(draw.lotteryId(), next, auth);
            for (AdminTicketView t : page.tickets()) {
                int matches = (int) t.numbers().stream().filter(winning::contains).count();
                if (tierByMatch.containsKey(matches)) {
                    hits.computeIfAbsent(matches, k -> new ArrayList<>()).add(t);
                }
            }
            scanned += page.tickets().size();
            next = page.nextAfterId();
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<Winner> toSave = new ArrayList<>();
        List<TierResult> results = new ArrayList<>();

        tiers.stream().sorted(Comparator.comparingInt(PrizeTierView::tierNumber)).forEach(tier -> {
            List<AdminTicketView> winners = hits.getOrDefault(tier.matchCount(), List.of());
            BigDecimal pool = tier.prizeAmount();
            if (winners.isEmpty()) {
                results.add(new TierResult(tier.tierNumber(), tier.tierName(),
                        tier.matchCount(), pool, 0, BigDecimal.ZERO, pool));
                return;
            }
            // Pool split equally, rounded DOWN so we never pay out more than the pool
            BigDecimal share = pool.divide(BigDecimal.valueOf(winners.size()), 2, RoundingMode.DOWN);
            BigDecimal unpaid = pool.subtract(share.multiply(BigDecimal.valueOf(winners.size())));
            for (AdminTicketView t : winners) {
                Winner w = new Winner();
                w.setWinnerId(newWinnerId());
                w.setDrawId(draw.id());
                w.setTicketId(t.id());
                w.setLotteryId(draw.lotteryId());
                w.setCustomerId(t.customerId());
                w.setPrizeTierId(tier.id());
                w.setPrizeAmount(share);
                w.setStatus("DECLARED");
                w.setDeclaredAt(now);
                toSave.add(w);
            }
            results.add(new TierResult(tier.tierNumber(), tier.tierName(),
                    tier.matchCount(), pool, winners.size(), share, unpaid));
        });

        final long scannedFinal = scanned;
        final BigDecimal totalPaid = results.stream()
                .map(t -> t.amountPerWinner().multiply(BigDecimal.valueOf(t.winners())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try {
            // The winners and their audit record are saved together or not at all
            tx.executeWithoutResult(status -> {
                winnerRepository.saveAll(toSave);
                auditClient.record(adminUserId, "WINNERS_DECLARED", "DRAW", draw.drawId(), null,
                        Map.of("lotteryId", draw.lotteryId(),
                                "ticketsScanned", scannedFinal,
                                "totalWinners", toSave.size(),
                                "totalPaid", totalPaid));
            });
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException(
                    "Winners have already been declared for this draw");
        }
        log.info("Declared {} winners for draw {} (lottery {}) by user {}",
                toSave.size(), draw.drawId(), draw.lotteryId(), adminUserId);
        return new DeclareResult(draw.drawId(), draw.lotteryId(), scanned,
                toSave.size(), results);
    }

    public List<WinnerResponse> forDraw(String drawCode, String auth) {
        DrawView draw = clients.getDraw(drawCode, auth);
        return winnerRepository.findByDrawIdOrderByPrizeAmountDescIdAsc(draw.id())
                .stream().map(WinnerResponse::from).toList();
    }

    public List<WinnerResponse> mine(Long customerId) {
        return winnerRepository.findByCustomerIdOrderByDeclaredAtDesc(customerId)
                .stream().map(WinnerResponse::from).toList();
    }

    private String newWinnerId() {
        for (int i = 0; i < MAX_ID_ATTEMPTS; i++) {
            String id = codeGenerator.next("WIN-");
            if (!winnerRepository.existsByWinnerId(id)) {
                return id;
            }
        }
        throw new IllegalStateException("Could not generate a unique winner id");
    }

}