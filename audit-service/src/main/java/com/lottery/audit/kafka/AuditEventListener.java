package com.lottery.audit.kafka;

import com.lottery.audit.dto.AuditEventRequest;
import com.lottery.audit.exception.DuplicateResourceException;
import com.lottery.audit.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final AuditService auditService;

    public AuditEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @KafkaListener(topics = "${audit.kafka.topic:audit-events}")
    public void onMessage(String payload) {
        AuditEventRequest request = MAPPER.readValue(payload, AuditEventRequest.class);

        // The event id is required, because it makes a repeated delivery harmless
        if (isBlank(request.eventId()) || isBlank(request.serviceName())
                || isBlank(request.action())) {
            throw new IllegalArgumentException(
                    "eventId, serviceName and action are required");
        }

        try {
            auditService.record(request);
        } catch (DuplicateResourceException e) {
            // Already stored. Kafka can deliver a message twice, so this is normal
            log.debug("Audit event {} was already recorded", request.eventId());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}