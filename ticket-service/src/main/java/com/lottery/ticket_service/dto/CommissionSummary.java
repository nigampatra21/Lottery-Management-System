package com.lottery.ticket_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record CommissionSummary(
        BigDecimal totalEarned, BigDecimal totalReversed,
        List<CommissionResponse> items) {}