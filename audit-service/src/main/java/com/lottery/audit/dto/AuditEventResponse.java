package com.lottery.audit.dto;

import com.lottery.audit.entity.AuditEvent;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

public record AuditEventResponse(
        String eventId, String serviceName, Long userId, String action,
        String resourceType, String resourceId,
        Map<String, Object> oldValue, Map<String, Object> newValue,
        String ipAddress, String correlationId, Instant createdAt) {

    public static AuditEventResponse from(AuditEvent e) {
        return new AuditEventResponse(e.getEventId(), e.getServiceName(), e.getUserId(),
                e.getAction(), e.getResourceType(), e.getResourceId(),
                e.getOldValue(), e.getNewValue(), e.getIpAddress(),
                e.getCorrelationId(), e.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
}