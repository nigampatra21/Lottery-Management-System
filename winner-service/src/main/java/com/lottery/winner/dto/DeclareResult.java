package com.lottery.winner.dto;

import java.util.List;

public record DeclareResult(String drawId, Long lotteryId, long ticketsScanned,
                            int totalWinners, List<TierResult> tiers) {}