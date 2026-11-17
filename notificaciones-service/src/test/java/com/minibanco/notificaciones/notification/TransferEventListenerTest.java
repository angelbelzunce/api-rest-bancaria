package com.minibanco.notificaciones.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TransferEventListenerTest {

    @Mock
    NotificationService notificationService;

    @InjectMocks
    TransferEventListener transferEventListener;

    @Test
    void onTransfer_savesNotificationWithTransferId() {
        transferEventListener.onTransfer("42");

        verify(notificationService).save(new Notification(42L, "Transfer completed"));
    }
}
