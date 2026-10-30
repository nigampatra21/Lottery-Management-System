package com.lottery.ticket_service.repository;

import com.lottery.ticket_service.entity.Ticket;
import com.lottery.ticket_service.entity.TicketStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByCustomerIdOrderByPurchasedAtDesc(Long customerId);

    Optional<Ticket> findByIdAndCustomerId(Long id, Long customerId);

    boolean existsByTicketId(String ticketId);

    boolean existsByLotteryIdAndTicketNumber(Long lotteryId, String ticketNumber);

    long countByLotteryIdAndStatus(Long lotteryId, TicketStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.id = :id and t.customerId = :customerId")
    Optional<Ticket> findByIdAndCustomerIdForUpdate(@Param("id") Long id,
                                                    @Param("customerId") Long customerId);
    List<Ticket> findByLotteryIdAndStatusAndIdGreaterThanOrderByIdAsc(
            Long lotteryId, TicketStatus status, Long afterId, Limit limit);
    Optional<Ticket> findByTicketId(String ticketId);
    boolean existsByLotteryIdAndTicketNumberAndStatusIn(
            Long lotteryId, String ticketNumber, Collection<TicketStatus> statuses);

    long countByLotteryIdAndStatusIn(Long lotteryId, Collection<TicketStatus> statuses);

    long countByCustomerIdAndStatus(Long customerId, TicketStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findByIdForUpdate(@Param("id") Long id);

    List<Ticket> findTop50ByStatusAndPurchasedAtBeforeOrderByIdAsc(
            TicketStatus status, LocalDateTime before);
    List<Ticket> findByAgentIdOrderByPurchasedAtDesc(Long agentId);

    Optional<Ticket> findByIdAndAgentId(Long id, Long agentId);


}