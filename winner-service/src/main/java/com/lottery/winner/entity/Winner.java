package com.lottery.winner.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "winners")
public class Winner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "winner_id", nullable = false, unique = true, length = 50)
    private String winnerId;

    // draws.id in draw-service (no foreign key across databases)
    @Column(name = "draw_id", nullable = false)
    private Long drawId;

    // tickets.id in ticket-service
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "lottery_id", nullable = false)
    private Long lotteryId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "prize_tier_id")
    private Long prizeTierId;

    @Column(name = "prize_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal prizeAmount;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DECLARED";

    @Column(name = "declared_at", nullable = false)
    private LocalDateTime declaredAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // Generate getters and setters for all fields

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    public Long getDrawId() {
        return drawId;
    }

    public void setDrawId(Long drawId) {
        this.drawId = drawId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getLotteryId() {
        return lotteryId;
    }

    public void setLotteryId(Long lotteryId) {
        this.lotteryId = lotteryId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getPrizeTierId() {
        return prizeTierId;
    }

    public void setPrizeTierId(Long prizeTierId) {
        this.prizeTierId = prizeTierId;
    }

    public BigDecimal getPrizeAmount() {
        return prizeAmount;
    }

    public void setPrizeAmount(BigDecimal prizeAmount) {
        this.prizeAmount = prizeAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDeclaredAt() {
        return declaredAt;
    }

    public void setDeclaredAt(LocalDateTime declaredAt) {
        this.declaredAt = declaredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    // (IntelliJ: Alt+Insert → Getter and Setter → select all)
}