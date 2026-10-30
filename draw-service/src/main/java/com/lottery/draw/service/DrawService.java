package com.lottery.draw.service;

import com.lottery.draw.audit.AuditClient;
import com.lottery.draw.client.LotteryClient;
import com.lottery.draw.dto.DrawRequest;
import com.lottery.draw.dto.DrawResponse;
import com.lottery.draw.dto.LotteryPurchaseInfo;
import com.lottery.draw.entity.Draw;
import com.lottery.draw.entity.DrawStatus;
import com.lottery.draw.entity.WinningNumber;
import com.lottery.draw.exception.BusinessRuleException;
import com.lottery.draw.exception.DuplicateResourceException;
import com.lottery.draw.exception.ResourceNotFoundException;
import com.lottery.draw.repository.DrawRepository;
import com.lottery.draw.repository.WinningNumberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DrawService {

    private static final Logger log = LoggerFactory.getLogger(DrawService.class);
    // Zone the lottery's sales/draw dates are entered in
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int MAX_ID_ATTEMPTS = 5;

    private final DrawRepository drawRepository;
    private final WinningNumberRepository numberRepository;
    private final LotteryClient lotteryClient;
    private final WinningNumberGenerator numberGenerator;
    private final CodeGenerator codeGenerator;
    private final TransactionTemplate tx;
    private final AuditClient auditClient;

    public DrawService(DrawRepository drawRepository,
                       WinningNumberRepository numberRepository,
                       LotteryClient lotteryClient,
                       WinningNumberGenerator numberGenerator,
                       CodeGenerator codeGenerator,
                       TransactionTemplate tx,
                       AuditClient auditClient) {
        this.drawRepository = drawRepository;
        this.numberRepository = numberRepository;
        this.lotteryClient = lotteryClient;
        this.numberGenerator = numberGenerator;
        this.codeGenerator = codeGenerator;
        this.tx = tx;
        this.auditClient = auditClient;
    }

    public DrawResponse execute(Long adminUserId, String authHeader, DrawRequest req) {
        // Remote call first, outside any database transaction
        LotteryPurchaseInfo info =
                lotteryClient.getPurchaseInfo(req.lotteryId(), authHeader);

        if ("DRAFT".equals(info.status()) || "CANCELLED".equals(info.status())) {
            throw new BusinessRuleException(
                    "A draw cannot be held for a lottery with status " + info.status());
        }
        // Numbers must never exist while tickets are still on sale
        if (LocalDateTime.now(BUSINESS_ZONE).isBefore(info.salesEndAt())) {
            throw new BusinessRuleException("Ticket sales have not ended yet");
        }
        if (drawRepository.existsByLotteryId(info.lotteryId())) {
            throw new DuplicateResourceException(
                    "A draw has already been held for this lottery");
        }

        List<Integer> numbers = numberGenerator.generate(
                info.numberCount(), info.minNumber(), info.maxNumber());

        // All draw datetimes are stored in UTC, like ticket timestamps
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Draw draw = new Draw();
        draw.setDrawId(uniqueDrawId());
        draw.setLotteryId(info.lotteryId());
        draw.setScheduledAt(info.drawAt().atZone(BUSINESS_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
        draw.setStartedAt(now);
        draw.setCompletedAt(now);
        draw.setStatus(DrawStatus.COMPLETED);
        draw.setExecutionReference("SECURE_RANDOM:" + UUID.randomUUID());

        try {
            // The draw, its numbers and its audit record are saved together or not at all
            DrawResponse response = tx.execute(status -> {
                Draw saved = drawRepository.save(draw);
                List<WinningNumber> rows = new ArrayList<>();
                for (int i = 0; i < numbers.size(); i++) {
                    WinningNumber w = new WinningNumber();
                    w.setDrawId(saved.getId());
                    w.setSequenceNumber(i + 1);
                    w.setWinningNumber(numbers.get(i));
                    rows.add(w);
                }
                numberRepository.saveAll(rows);

                auditClient.record(adminUserId, "DRAW_EXECUTED", "DRAW", saved.getDrawId(),
                        null,
                        Map.of("lotteryId", info.lotteryId(),
                                "winningNumbers", numbers,
                                "executionReference", saved.getExecutionReference()));
                return DrawResponse.from(saved, numbers);
            });
            log.info("Draw {} for lottery {} executed by user {}",
                    draw.getDrawId(), info.lotteryId(), adminUserId);
            return response;
        } catch (DataIntegrityViolationException e) {
            // Another admin ran the same lottery's draw at the same moment
            throw new DuplicateResourceException(
                    "A draw has already been held for this lottery");
        }
    }

    public DrawResponse getByDrawId(String drawId) {
        Draw draw = drawRepository.findByDrawId(drawId)
                .orElseThrow(() -> new ResourceNotFoundException("Draw not found"));
        return toResponse(draw);
    }

    public DrawResponse getByLottery(Long lotteryId) {
        Draw draw = drawRepository.findByLotteryId(lotteryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No draw has been held for this lottery"));
        return toResponse(draw);
    }

    private DrawResponse toResponse(Draw draw) {
        List<Integer> numbers = numberRepository
                .findByDrawIdOrderBySequenceNumberAsc(draw.getId())
                .stream().map(WinningNumber::getWinningNumber).toList();
        return DrawResponse.from(draw, numbers);
    }

    private String uniqueDrawId() {
        for (int i = 0; i < MAX_ID_ATTEMPTS; i++) {
            String id = codeGenerator.next("DRW-");
            if (!drawRepository.existsByDrawId(id)) {
                return id;
            }
        }
        throw new IllegalStateException("Could not generate a unique draw id");
    }
}