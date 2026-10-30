//package com.lottery.winner.audit;
//
//import jakarta.persistence.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "outbox_events")
//public class OutboxEvent {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "event_id", nullable = false, unique = true, length = 100)
//    private String eventId;
//
//    @Column(name = "topic", nullable = false, length = 100)
//    private String topic;
//
//    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
//    private String payload;
//
//    // PENDING, SENT, or FAILED (gave up after too many tries: needs a person to look)
//    @Column(name = "status", nullable = false, length = 20)
//    private String status = "PENDING";
//
//    @Column(name = "attempts", nullable = false)
//    private int attempts;
//
//    @Column(name = "last_error", length = 500)
//    private String lastError;
//
//    @Column(name = "created_at", nullable = false)
//    private LocalDateTime createdAt;
//
//    @Column(name = "sent_at")
//    private LocalDateTime sentAt;
//
//    // Generate getters and setters for all fields
//    // (IntelliJ: Alt+Insert, then Getter and Setter, then select all)
//}