package com.minibanco.notifications.notification;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository){
        this.notificationRepository = notificationRepository;
    }

    public Notification save(Long transferId, String message){
        return notificationRepository.save(new Notification(transferId, message));
    }

    public List<Notification> findAll() {
        return notificationRepository.findAll();
    }

    public void saveIfAbsent(Long transferId){

        if (!notificationRepository.existsByTransferId(transferId)){
            save(transferId, "Transfer completed");
        }
    }
}
