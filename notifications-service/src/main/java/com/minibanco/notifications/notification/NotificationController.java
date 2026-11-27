package com.minibanco.notifications.notification;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@RequestBody @Valid CreateNotificationRequest request) {
        Notification saved = notificationService.save(request.transferId(), request.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificationResponse.from(saved));

    }

    @GetMapping
    public List<NotificationResponse> findAll(){

        return notificationService.findAll().stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
