package com.payforge.commons.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Moves outbox rows to Kafka. Publishing is at-least-once: a crash between the broker ack
 * and this transaction's commit replays the event, so consumers must be idempotent.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repository;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate;
    private final String topicPrefix;
    private final int batchSize;

    public OutboxRelay(OutboxRepository repository,
                       ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate,
                       @Value("${payforge.outbox.topic-prefix:payforge}") String topicPrefix,
                       @Value("${payforge.outbox.batch-size:50}") int batchSize) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.topicPrefix = topicPrefix;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${payforge.outbox.interval-ms:1000}")
    @Transactional
    public void relay() {
        List<OutboxEntry> entries = repository.claimPending(batchSize);
        if (entries.isEmpty()) {
            return;
        }

        KafkaTemplate<String, String> template = kafkaTemplate.getIfAvailable();
        if (template == null) {
            log.debug("No KafkaTemplate on the classpath; leaving {} outbox entries pending", entries.size());
            return;
        }

        for (OutboxEntry entry : entries) {
            String topic = topicPrefix + "." + entry.getAggregateType().toLowerCase() + ".events";
            try {
                template.send(topic, entry.getAggregateId(), entry.getPayload()).get();
                entry.markPublished();
            } catch (Exception e) {
                entry.recordFailedAttempt();
                log.warn("Outbox publish failed for entry {} on topic {} (attempt {})",
                        entry.getId(), topic, entry.getAttempts(), e);
            }
        }
    }
}
