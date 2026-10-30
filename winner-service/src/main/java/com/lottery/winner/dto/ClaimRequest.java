package com.lottery.winner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ClaimRequest(
        @NotBlank @Schema(example = "WIN-ABCDEFGHJKLM",
                description = "The public winner id from GET /api/winners/my") String winnerId) {}