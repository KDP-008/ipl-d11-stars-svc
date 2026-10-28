package com.ipld11stars.backend.controller;

import com.ipld11stars.backend.model.EmailNotification;
import com.ipld11stars.backend.notification.NotificationService;
import com.ipld11stars.backend.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<EmailNotification> myNotifications() {
        return notificationService.getForUser(CurrentUser.requireId());
    }
}
