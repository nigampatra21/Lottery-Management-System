package com.lottery.draw.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "winning_numbers")
public class WinningNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Points to draws.id (enforced by the database foreign key)
    @Column(name = "draw_id", nullable = false)
    private Long drawId;

    // Order in which the number was drawn: 1, 2, 3...
    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "winning_number", nullable = false)
    private int winningNumber;

    // Generate getters and setters for all fields

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDrawId() {
        return drawId;
    }

    public void setDrawId(Long drawId) {
        this.drawId = drawId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getWinningNumber() {
        return winningNumber;
    }

    public void setWinningNumber(int winningNumber) {
        this.winningNumber = winningNumber;
    }
}