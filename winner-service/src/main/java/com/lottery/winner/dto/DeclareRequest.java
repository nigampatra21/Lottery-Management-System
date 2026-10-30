package com.lottery.winner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DeclareRequest(
        @NotBlank @Schema(example = "DRW-85UFHY46R77M",
                description = "The public draw code") String drawId) {}