package com.minibanco.notificaciones.notification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByTransferId(Long transferId);
}
