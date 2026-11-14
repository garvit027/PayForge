package com.payforge.notifications.repo;

import com.payforge.notifications.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByEventKey(String eventKey);

    List<Notification> findTop50BySourceAccountOrTargetAccountOrderByCreatedAtDesc(
            String sourceAccount, String targetAccount);

    List<Notification> findTop50ByOrderByCreatedAtDesc();
}
