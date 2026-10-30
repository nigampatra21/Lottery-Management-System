package com.lottery.ticket_service.repository;

import com.lottery.ticket_service.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;

public interface TicketTransactionRepository extends JpaRepository<TicketTransaction, Long> {

    List<TicketTransaction> findByTicketIdOrderByIdAsc(Long ticketId);

    Optional<TicketTransaction> findFirstByTicketIdAndTransactionTypeOrderByIdAsc(
            Long ticketId, TransactionType type);
    Optional<TicketTransaction> findByGatewayOrderId(String gatewayOrderId);
    Optional<TicketTransaction> findFirstByTicketIdAndTransactionTypeAndPaymentMethodOrderByIdAsc(
            Long ticketId,
            TransactionType transactionType,
            PaymentMethod paymentMethod
    );
    List<TicketTransaction> findTop50ByTransactionTypeAndStatusAndPaymentMethodOrderByIdAsc(
            TransactionType type, TransactionStatus status, PaymentMethod method);

}