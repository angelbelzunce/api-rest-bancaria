package com.minibanco.notificaciones.notification;

import java.time.LocalDateTime;

public record CreateNotificationRequest(Long transferId, String message) {
}
