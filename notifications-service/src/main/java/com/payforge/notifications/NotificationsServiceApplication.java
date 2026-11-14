package com.payforge.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scans only the shared web and Kafka helpers from {@code commons}: this service consumes
 * events, so the outbox (a producer-side concern) must stay out of its persistence unit.
 */
@SpringBootApplication(scanBasePackages = {
        "com.payforge.notifications",
        "com.payforge.commons.kafka",
        "com.payforge.commons.web"
})
@EntityScan(basePackages = "com.payforge.notifications.domain")
@EnableJpaRepositories(basePackages = "com.payforge.notifications.repo")
@EnableKafka
@EnableScheduling
public class NotificationsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationsServiceApplication.class, args);
    }
}
