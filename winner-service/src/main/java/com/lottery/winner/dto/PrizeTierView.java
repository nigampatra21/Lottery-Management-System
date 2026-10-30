package com.lottery.winner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PrizeTierView(Long id, int tierNumber, int matchCount,
                            String tierName, BigDecimal prizeAmount) {}