package com.lottery.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record AuditEventRequest(
        @Size(max = 100)
        @Schema(example = "EVT-TEST-0002",
                description = "Optional. A repeated id is rejected, so retries are safe")
        String eventId,
        @NotBlank @Size(max = 100) @Schema(example = "lottery-service") String serviceName,
        @Schema(example = "1", description = "User who performed the action") Long userId,
        @NotBlank @Size(max = 100) @Schema(example = "LOTTERY_UPDATED") String action,
        @Size(max = 100) @Schema(example = "LOTTERY") String resourceType,
        @Size(max = 100) @Schema(example = "3") String resourceId,
        @Schema(example = "{\"ticketPrice\": 100.00}") Map<String, Object> oldValue,
        @Schema(example = "{\"ticketPrice\": 150.00}") Map<String, Object> newValue,
        @Size(max = 45) @Schema(example = "203.0.113.10") String ipAddress,
        @Size(max = 100) @Schema(example = "req-12345") String correlationId) {}