package com.minibanco.cuentas.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TransferEventPublisher {
    private static final String TOPIC = "transfers";
    private static final Logger log = LoggerFactory.getLogger(TransferEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public TransferEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Long transferId) {

        try {
            kafkaTemplate.send(TOPIC, String.valueOf(transferId));

        } catch (Exception e) {
            // If Kafka is down the transfer is already done, so we only log it
            log.warn("Could not publish event for transfer " + transferId);

        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferCompleted(TransferCompletedEvent event){
        publish(event.transferId());
    }
}
