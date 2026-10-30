package com.lottery.ticket_service.repository;

import com.lottery.ticket_service.entity.AgentCommission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AgentCommissionRepository extends JpaRepository<AgentCommission, Long> {
    Optional<AgentCommission> findByTicketId(Long ticketId);
    List<AgentCommission> findByTicketIdIn(Collection<Long> ticketIds);
    List<AgentCommission> findByAgentIdOrderByIdDesc(Long agentId);
}