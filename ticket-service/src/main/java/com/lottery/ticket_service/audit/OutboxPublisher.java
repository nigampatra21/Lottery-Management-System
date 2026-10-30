package com.lottery.ticket_service.audit;

import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafka;

    public OutboxPublisher(OutboxEventRepository repository,
                           KafkaTemplate<String, String> kafka) {
        this.repository = repository;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelay = 2000)
    public void publishPending() {
        List<OutboxEvent> batch = repository.findTop50ByStatusOrderByIdAsc("PENDING");
        for (OutboxEvent event : batch) {
            try {
                // Wait for Kafka to confirm before marking the row as sent
                kafka.send(event.getTopic().trim(), event.getEventId(), event.getPayload())
                        .get(20, TimeUnit.SECONDS);
                event.setStatus("SENT");
                event.setSentAt(LocalDateTime.now(ZoneOffset.UTC));
                event.setLastError(null);
                repository.save(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                Throwable root = rootCause(e);
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(truncate(root.getClass().getSimpleName()
                        + ": " + root.getMessage()));

                if (isPermanent(root)) {
                    // This event can never be sent, so don't let it block the others
                    event.setStatus("FAILED");
                    repository.save(event);
                    log.error("Outbox event {} can never be sent and is marked FAILED: {}",
                            event.getEventId(), event.getLastError());
                    continue;
                }

                // Kafka is probably unreachable. Keep the event and try again later
                repository.save(event);
                if (event.getAttempts() == 1 || event.getAttempts() % 15 == 0) {
                    log.warn("Outbox event {} not sent yet (attempt {}): {}",
                            event.getEventId(), event.getAttempts(), event.getLastError());
                }
                return;
            }
        }
    }

    private static boolean isPermanent(Throwable t) {
        return t instanceof RecordTooLargeException
                || t instanceof InvalidTopicException
                || t instanceof SerializationException;
    }

    private static Throwable rootCause(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private static String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > 500 ? text.substring(0, 500) : text;
    }
}