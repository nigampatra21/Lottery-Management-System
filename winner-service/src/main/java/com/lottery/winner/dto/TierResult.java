package com.lottery.winner.dto;

import java.math.BigDecimal;

public record TierResult(int tierNumber, String tierName, int matchCount,
                         BigDecimal pool, int winners,
                         BigDecimal amountPerWinner, BigDecimal unpaid) {}