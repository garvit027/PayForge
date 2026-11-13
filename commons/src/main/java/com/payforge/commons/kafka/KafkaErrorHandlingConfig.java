package com.payforge.commons.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Shared consumer resilience: a few in-place retries, then a dead-letter topic named
 * {@code <topic>.dlt}. Dead-lettered records are kept so an operator can inspect and replay them.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".dlt", record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(500L, 3L));
        handler.setCommitRecovered(true);
        return handler;
    }

    /**
     * Local convenience so a fresh broker has the payment topics. In production the broker
     * is provisioned separately and applications do not own topic lifecycle.
     */
    @Bean
    public NewTopic transferEventsTopic() {
        return TopicBuilder.name("payforge.transfer.events").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic accountEventsTopic() {
        return TopicBuilder.name("payforge.account.events").partitions(3).replicas(1).build();
    }
}
