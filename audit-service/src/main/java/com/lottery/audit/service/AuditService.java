package com.lottery.audit.service;

import com.lottery.audit.dto.AuditEventRequest;
import com.lottery.audit.dto.AuditEventResponse;
import com.lottery.audit.dto.PageResponse;
import com.lottery.audit.entity.AuditEvent;
import com.lottery.audit.exception.BusinessRuleException;
import com.lottery.audit.exception.DuplicateResourceException;
import com.lottery.audit.exception.ResourceNotFoundException;
import com.lottery.audit.repository.AuditEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public AuditEventResponse record(AuditEventRequest req) {
        String eventId = (req.eventId() == null || req.eventId().isBlank())
                ? "EVT-" + UUID.randomUUID()
                : req.eventId();
        if (repository.existsByEventId(eventId)) {
            throw new DuplicateResourceException("This event has already been recorded");
        }

        AuditEvent e = new AuditEvent();
        e.setEventId(eventId);
        e.setServiceName(req.serviceName());
        e.setUserId(req.userId());
        e.setAction(req.action());
        e.setResourceType(req.resourceType());
        e.setResourceId(req.resourceId());
        e.setOldValue(req.oldValue());
        e.setNewValue(req.newValue());
        e.setIpAddress(req.ipAddress());
        e.setCorrelationId(req.correlationId());

        try {
            return AuditEventResponse.from(repository.save(e));
        } catch (DataIntegrityViolationException ex) {
            // Same event sent twice at the same moment: the unique key stopped the second
            throw new DuplicateResourceException("This event has already been recorded");
        }
    }

    public AuditEventResponse getByEventId(String eventId) {
        return repository.findByEventId(eventId)
                .map(AuditEventResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Audit event not found"));
    }

    public PageResponse<AuditEventResponse> search(
            Long userId, String action, String resourceType, String resourceId,
            String serviceName, String correlationId,
            LocalDateTime from, LocalDateTime to, int page, int size) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException("'from' must not be after 'to'");
        }

        Specification<AuditEvent> spec = (root, query, cb) -> cb.conjunction();
        if (userId != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("userId"), userId));
        }
        if (notBlank(action)) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("action"), action));
        }
        if (notBlank(resourceType)) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("resourceType"), resourceType));
        }
        if (notBlank(resourceId)) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("resourceId"), resourceId));
        }
        if (notBlank(serviceName)) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("serviceName"), serviceName));
        }
        if (notBlank(correlationId)) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("correlationId"), correlationId));
        }
        if (from != null) {
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("createdAt"), to));
        }

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Page<AuditEvent> result = repository.findAll(spec,
                PageRequest.of(safePage, safeSize,
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        return new PageResponse<>(
                result.getContent().stream().map(AuditEventResponse::from).toList(),
                result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}