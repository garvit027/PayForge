package com.payforge.notifications.web;

import com.payforge.notifications.service.NotificationService;
import com.payforge.notifications.web.dto.NotificationView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Newest first. Without an account filter this returns the platform-wide feed, which is an
     * operator view; the UI passes the signed-in customer's account number.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('SCOPE_notifications:read', 'SCOPE_platform:admin')")
    public List<NotificationView> feed(@RequestParam(required = false) String accountNumber) {
        return notificationService.feed(accountNumber);
    }
}
