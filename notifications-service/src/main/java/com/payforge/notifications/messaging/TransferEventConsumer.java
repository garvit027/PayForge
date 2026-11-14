package com.payforge.notifications.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payforge.notifications.service.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransferEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferEventConsumer.class);

    private final NotificationService notifications;
    private final ObjectMapper objectMapper;

    public TransferEventConsumer(NotificationService notifications, ObjectMapper objectMapper) {
        this.notifications = notifications;
        this.objectMapper = objectMapper;
    }

    /**
     * A malformed payload is thrown, not swallowed: the error handler retries and then moves the
     * record to {@code payforge.transfer.events.dlt} so it can be inspected instead of lost.
     */
    @KafkaListener(
            topics = "${payforge.topics.transfer-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "${payforge.consumer.concurrency:2}")
    public void onTransferEvent(ConsumerRecord<String, String> record) throws Exception {
        TransferEvent event = objectMapper.readValue(record.value(), TransferEvent.class);
        boolean stored = notifications.record(event);

        if (stored) {
            log.info("Projected {} for transfer {}", event.status(), event.transferId());
        } else {
            log.debug("Skipping already-projected event for transfer {} ({})",
                    event.transferId(), event.status());
        }
    }
}
