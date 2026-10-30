package com.lottery.audit.repository;

import com.lottery.audit.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AuditEventRepository
        extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

    boolean existsByEventId(String eventId);

    Optional<AuditEvent> findByEventId(String eventId);
}