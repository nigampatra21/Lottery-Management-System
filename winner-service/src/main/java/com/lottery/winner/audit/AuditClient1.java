//package com.lottery.winner.audit;
//
//import com.lottery.winner.security.AuthenticatedUser;
//import jakarta.servlet.http.HttpServletRequest;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.MediaType;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.support.TransactionSynchronization;
//import org.springframework.transaction.support.TransactionSynchronizationManager;
//import org.springframework.web.client.RestClient;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//@Component
//public class AuditClient1 {
//
//    private static final Logger log = LoggerFactory.getLogger(AuditClient1.class);
//
//    private final RestClient restClient;
//    private final ExecutorService executor;
//    private final String internalKey;
//    private final String serviceName;
//    private final AtomicBoolean warned = new AtomicBoolean(false);
//
//    public AuditClient1(@Qualifier("auditRestClient") RestClient restClient,
//                        @Qualifier("auditExecutor") ExecutorService executor,
//                        @Value("${audit.internal-key:}") String internalKey,
//                        @Value("${spring.application.name}") String serviceName) {
//        this.restClient = restClient;
//        this.executor = executor;
//        this.internalKey = internalKey;
//        this.serviceName = serviceName;
//    }
//
//    /** Uses the logged-in user from the security context. */
//    public void record(String action, String resourceType, String resourceId,
//                       Map<String, Object> oldValue, Map<String, Object> newValue) {
//        record(currentUserId(), action, resourceType, resourceId, oldValue, newValue);
//    }
//
//    /** Best effort: never throws, never blocks the caller. */
//    public void record(Long userId, String action, String resourceType, String resourceId,
//                       Map<String, Object> oldValue, Map<String, Object> newValue) {
//        if (internalKey.isBlank()) {
//            if (warned.compareAndSet(false, true)) {
//                log.warn("audit.internal-key is not set: audit events are not being sent");
//            }
//            return;
//        }
//
//        // Read request details now: the request is gone when the async task runs
//        String ip = null;
//        String correlationId = null;
//        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
//            HttpServletRequest request = attrs.getRequest();
//            ip = limit(request.getRemoteAddr(), 45);
//            correlationId = limit(request.getHeader("X-Correlation-Id"), 100);
//        }
//
//        Map<String, Object> body = new HashMap<>();
//        body.put("eventId", "EVT-" + UUID.randomUUID());
//        body.put("serviceName", serviceName);
//        body.put("userId", userId);
//        body.put("action", action);
//        body.put("resourceType", resourceType);
//        body.put("resourceId", resourceId);
//        body.put("oldValue", oldValue);
//        body.put("newValue", newValue);
//        body.put("ipAddress", ip);
//        body.put("correlationId", correlationId);
//
//        // Inside a transaction, send only after it commits (nothing is sent on rollback)
//        if (TransactionSynchronizationManager.isSynchronizationActive()) {
//            TransactionSynchronizationManager.registerSynchronization(
//                    new TransactionSynchronization() {
//                        @Override
//                        public void afterCommit() {
//                            submit(body);
//                        }
//                    });
//        } else {
//            submit(body);
//        }
//    }
//
//    private void submit(Map<String, Object> body) {
//        try {
//            executor.execute(() -> send(body));
//        } catch (RuntimeException e) {
//            log.warn("Audit event {} could not be queued: {}", body.get("action"), e.getMessage());
//        }
//    }
//
//    private void send(Map<String, Object> body) {
//        try {
//            restClient.post()
//                    .uri("/api/audit/events")
//                    .header("X-Internal-Key", internalKey)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .body(body)
//                    .retrieve()
//                    .toBodilessEntity();
//        } catch (Exception e) {
//            // Never log the body or the key
//            log.warn("Audit event {} could not be sent: {}", body.get("action"), e.getMessage());
//        }
//    }
//
//    private Long currentUserId() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
//            return user.userId();
//        }
//        return null;
//    }
//
//    private static String limit(String value, int max) {
//        if (value == null) {
//            return null;
//        }
//        return value.length() > max ? value.substring(0, max) : value;
//    }
//}