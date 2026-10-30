package com.lottery.draw.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record DrawRequest(@NotNull @Schema(example = "1") Long lotteryId) {}