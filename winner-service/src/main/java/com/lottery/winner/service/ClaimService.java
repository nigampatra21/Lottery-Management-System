package com.lottery.winner.service;

import com.lottery.winner.audit.AuditClient;
import com.lottery.winner.dto.ClaimRequest;
import com.lottery.winner.dto.ClaimResponse;
import com.lottery.winner.entity.PrizeClaim;
import com.lottery.winner.entity.Winner;
import com.lottery.winner.exception.BusinessRuleException;
import com.lottery.winner.exception.DuplicateResourceException;
import com.lottery.winner.exception.ResourceNotFoundException;
import com.lottery.winner.repository.PrizeClaimRepository;
import com.lottery.winner.repository.WinnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClaimService {

    private static final Logger log = LoggerFactory.getLogger(ClaimService.class);
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Set<String> STATUSES = Set.of("PENDING", "APPROVED", "REJECTED");

    private final PrizeClaimRepository claimRepository;
    private final WinnerRepository winnerRepository;
    private final TransactionTemplate tx;
    private final AuditClient auditClient;
    private final SecureRandom random = new SecureRandom();

    public ClaimService(PrizeClaimRepository claimRepository,
                        WinnerRepository winnerRepository,
                        TransactionTemplate tx,
                        AuditClient auditClient) {
        this.claimRepository = claimRepository;
        this.winnerRepository = winnerRepository;
        this.tx = tx;
        this.auditClient = auditClient;
    }

    public ClaimResponse submit(Long customerId, ClaimRequest req) {
        try {
            return tx.execute(status -> {
                // 404 (not 403) for other people's prizes, so winner ids can't be probed
                Winner winner = winnerRepository.findByWinnerId(req.winnerId())
                        .filter(w -> customerId.equals(w.getCustomerId()))
                        .orElseThrow(() -> new ResourceNotFoundException("Prize not found"));

                if (claimRepository.existsByWinnerId(winner.getId())) {
                    throw new DuplicateResourceException(
                            "A claim has already been submitted for this prize");
                }
                if (!"DECLARED".equals(winner.getStatus())) {
                    throw new BusinessRuleException("This prize cannot be claimed");
                }

                PrizeClaim claim = new PrizeClaim();
                claim.setWinnerId(winner.getId());
                claim.setClaimReference(newReference());
                claim.setStatus("PENDING");
                claim.setClaimedAt(LocalDateTime.now(ZoneOffset.UTC));
                claimRepository.save(claim);

                winner.setStatus("CLAIM_PENDING");
                winnerRepository.save(winner);

                ClaimResponse response = ClaimResponse.from(claim, winner);
                // Inside the transaction: saved together with the claim, or not at all
                auditClient.record(customerId, "CLAIM_SUBMITTED", "PRIZE_CLAIM",
                        response.claimReference(), null,
                        Map.of("winnerId", response.winnerId(),
                                "prizeAmount", response.prizeAmount(),
                                "status", response.status()));
                return response;
            });
        } catch (DataIntegrityViolationException e) {
            // Two simultaneous submissions: the unique key on winner_id stopped the second
            throw new DuplicateResourceException(
                    "A claim has already been submitted for this prize");
        }
    }
    public ClaimResponse decide(Long adminUserId, String claimReference, boolean approve) {
        ClaimResponse response = tx.execute(status -> {
            PrizeClaim claim = claimRepository
                    .findForUpdateByClaimReference(claimReference)
                    .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));
            if (!"PENDING".equals(claim.getStatus())) {
                throw new BusinessRuleException("This claim has already been decided");
            }
            Winner winner = winnerRepository.findById(claim.getWinnerId())
                    .orElseThrow(() -> new IllegalStateException("Winner missing for claim"));
            // Separation of duties
            if (adminUserId.equals(winner.getCustomerId())) {
                throw new BusinessRuleException("You cannot verify your own claim");
            }

            claim.setStatus(approve ? "APPROVED" : "REJECTED");
            claim.setVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
            claim.setVerifiedBy(adminUserId);
            winner.setStatus(approve ? "CLAIMED" : "CLAIM_REJECTED");
            claimRepository.save(claim);
            winnerRepository.save(winner);

            ClaimResponse result = ClaimResponse.from(claim, winner);
            // Inside the transaction, so the decision and its audit record cannot separate
            auditClient.record(adminUserId, approve ? "CLAIM_APPROVED" : "CLAIM_REJECTED",
                    "PRIZE_CLAIM", claimReference,
                    Map.of("status", "PENDING"),
                    Map.of("status", result.status(),
                            "winnerId", result.winnerId(),
                            "prizeAmount", result.prizeAmount()));
            return result;
        });

        log.info("Claim {} {} by user {}", claimReference,
                approve ? "approved" : "rejected", adminUserId);
        return response;
    }

    public List<ClaimResponse> mine(Long customerId) {
        Map<Long, Winner> winners = winnerRepository
                .findByCustomerIdOrderByDeclaredAtDesc(customerId).stream()
                .collect(Collectors.toMap(Winner::getId, Function.identity()));
        if (winners.isEmpty()) {
            return List.of();
        }
        return claimRepository.findByWinnerIdIn(winners.keySet()).stream()
                .map(c -> ClaimResponse.from(c, winners.get(c.getWinnerId())))
                .toList();
    }

    public List<ClaimResponse> byStatus(String status) {
        if (!STATUSES.contains(status)) {
            throw new BusinessRuleException("Status must be PENDING, APPROVED or REJECTED");
        }
        List<PrizeClaim> claims = claimRepository.findByStatusOrderByIdAsc(status);
        if (claims.isEmpty()) {
            return List.of();
        }
        Map<Long, Winner> winners = winnerRepository
                .findByIdIn(claims.stream().map(PrizeClaim::getWinnerId).toList())
                .stream().collect(Collectors.toMap(Winner::getId, Function.identity()));
        return claims.stream()
                .map(c -> ClaimResponse.from(c, winners.get(c.getWinnerId())))
                .toList();
    }

    private String newReference() {
        StringBuilder sb = new StringBuilder("CLM-");
        for (int i = 0; i < 12; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}