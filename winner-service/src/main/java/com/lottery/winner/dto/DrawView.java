package com.lottery.winner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DrawView(Long id, String drawId, Long lotteryId, String status,
                       List<Integer> winningNumbers) {}