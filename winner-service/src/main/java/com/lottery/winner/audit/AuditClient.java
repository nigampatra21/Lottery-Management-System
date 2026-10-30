package com.lottery.winner.audit;

import com.lottery.winner.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class AuditClient {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final OutboxEventRepository outbox;
    private final String serviceName;
    private final String topic;

    public AuditClient(OutboxEventRepository outbox,
                       @Value("${spring.application.name}") String serviceName,
                       @Value("${audit.kafka.topic:audit-events}") String topic) {
        this.outbox = outbox;
        this.serviceName = serviceName;
        this.topic = topic;
    }

    /** Uses the logged-in user from the security context. */
    public void record(String action, String resourceType, String resourceId,
                       Map<String, Object> oldValue, Map<String, Object> newValue) {
        record(currentUserId(), action, resourceType, resourceId, oldValue, newValue);
    }

    /**
     * Saves the event in the outbox. It joins the caller's transaction, so the event
     * exists only if the business change was saved too.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long userId, String action, String resourceType, String resourceId,
                       Map<String, Object> oldValue, Map<String, Object> newValue) {

        String ip = null;
        String correlationId = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            // Behind the gateway, the real client is in X-Forwarded-For
            String forwarded = request.getHeader("X-Forwarded-For");
            ip = limit(forwarded != null && !forwarded.isBlank()
                    ? forwarded.split(",")[0].trim()
                    : request.getRemoteAddr(), 45);
            correlationId = limit(request.getHeader("X-Correlation-Id"), 100);
        }

        String eventId = "EVT-" + UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", eventId);
        body.put("serviceName", serviceName);
        body.put("userId", userId);
        body.put("action", action);
        body.put("resourceType", resourceType);
        body.put("resourceId", resourceId);
        body.put("oldValue", oldValue);
        body.put("newValue", newValue);
        body.put("ipAddress", ip);
        body.put("correlationId", correlationId);

        OutboxEvent event = new OutboxEvent();
        event.setEventId(eventId);
        event.setTopic(topic);
        event.setPayload(MAPPER.writeValueAsString(body));
        event.setStatus("PENDING");
        event.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        outbox.save(event);
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId();
        }
        return null;
    }

    private static String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}