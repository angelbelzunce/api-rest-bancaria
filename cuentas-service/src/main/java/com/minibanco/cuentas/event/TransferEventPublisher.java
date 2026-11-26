package com.minibanco.cuentas.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransferEventPublisher {
    private static final String TOPIC = "transfers";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public TransferEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Long transferId) {

        kafkaTemplate.send(TOPIC, String.valueOf(transferId)).join();

    }
}
