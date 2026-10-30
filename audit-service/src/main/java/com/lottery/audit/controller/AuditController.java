package com.lottery.audit.controller;

import com.lottery.audit.dto.AuditEventRequest;
import com.lottery.audit.dto.AuditEventResponse;
import com.lottery.audit.dto.PageResponse;
import com.lottery.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit/events")
@Tag(name = "Audit events", description = "Append-only log. No update or delete exists")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(summary = "Record an audit event",
            description = "Internal services only. Authenticate with the X-Internal-Key "
                    + "header. A user token, even an admin's, returns 403. "
                    + "A repeated eventId returns 409",
            security = @SecurityRequirement(name = "internalKey"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditEventResponse record(@Valid @RequestBody AuditEventRequest request) {
        return auditService.record(request);
    }

    @Operation(summary = "Search audit events",
            description = "Roles: ADMIN or AUDITOR. Newest first, page size capped at 100. "
                    + "All filters are optional and combined with AND",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public PageResponse<AuditEventResponse> search(
            @Parameter(description = "Who performed the action")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "For example LOTTERY_UPDATED")
            @RequestParam(required = false) String action,
            @Parameter(description = "For example LOTTERY")
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @Parameter(description = "For example lottery-service")
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String correlationId,
            @Parameter(description = "UTC, for example 2026-10-18T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "UTC, for example 2026-10-19T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditService.search(userId, action, resourceType, resourceId,
                serviceName, correlationId, from, to, page, size);
    }

    @Operation(summary = "Get one audit event by its event id",
            description = "Roles: ADMIN or AUDITOR",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{eventId}")
    public AuditEventResponse get(@PathVariable String eventId) {
        return auditService.getByEventId(eventId);
    }
}