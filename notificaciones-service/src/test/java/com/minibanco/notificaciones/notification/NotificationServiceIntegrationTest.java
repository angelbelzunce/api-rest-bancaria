package com.minibanco.notificaciones.notification;

import com.minibanco.notificaciones.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
public class NotificationServiceIntegrationTest {

    @Autowired
    NotificationService notificationService;

    @Autowired
    NotificationRepository notificationRepository;

    @BeforeEach
    void cleanDatabase() {
        notificationRepository.deleteAll();
    }

    @Test
    void save_storesNotificationInDatabase(){
        notificationService.save(42L, "Transfer completed");

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        assertEquals(42L, notifications.getFirst().getTransferId());
        assertNotNull(notifications.getFirst().getCreatedAt());
    }

    @Test
    void save_whenTransferIdAlreadyExists_throwsDataIntegrityViolationException(){
        notificationService.save(42L, "Transfer completed");

        assertThrows(DataIntegrityViolationException.class,
                () -> notificationService.save(42L, "Transfer completed"));

    }
}
