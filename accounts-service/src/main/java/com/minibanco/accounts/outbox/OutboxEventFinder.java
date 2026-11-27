package com.minibanco.accounts.outbox;


import com.minibanco.accounts.event.TransferEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;



@Component
public class OutboxEventFinder {

    private static final Logger log = LoggerFactory.getLogger(TransferEventPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final TransferEventPublisher transferEventPublisher;

    public OutboxEventFinder(OutboxEventRepository outboxEventRepository, TransferEventPublisher transferEventPublisher) {
        this.outboxEventRepository = outboxEventRepository;
        this.transferEventPublisher = transferEventPublisher;
    }

    @Scheduled(fixedDelay = 5000L)
    public void sendEvents(){
        List<OutboxEvent> outboxEventList = outboxEventRepository.findBySentAtIsNullOrderByCreatedAt();

        for (OutboxEvent event : outboxEventList){
            try {
                transferEventPublisher.publish(event.getTransferId());
                event.markAsSent(LocalDateTime.now());
                outboxEventRepository.save(event);
            } catch (Exception e){
                log.warn("Could not publish event for transfer " + event.getTransferId());
                break;
            }

        }

    }
}
