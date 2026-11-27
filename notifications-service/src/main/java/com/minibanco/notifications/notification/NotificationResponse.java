package com.minibanco.notifications.notification;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, Long transferId, String message, LocalDateTime createdAt) {

    public static NotificationResponse from(Notification notification){
        return new NotificationResponse(notification.getId(), notification.getTransferId(),
                notification.getMessage(),notification.getCreatedAt());
    }
}
