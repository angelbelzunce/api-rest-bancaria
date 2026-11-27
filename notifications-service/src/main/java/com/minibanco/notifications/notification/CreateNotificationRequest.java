package com.minibanco.notifications.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(@NotNull Long transferId,
                                        @NotBlank @Size(max = 255) String message) {
}
