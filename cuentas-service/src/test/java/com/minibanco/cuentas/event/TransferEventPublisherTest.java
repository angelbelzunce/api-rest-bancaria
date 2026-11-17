package com.minibanco.cuentas.event;

import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransferEventPublisherTest {

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    TransferEventPublisher transferEventPublisher;

    @Test
    void publish_sendsTransferIdToTransfersTopic() {
        transferEventPublisher.publish(42L);

        verify(kafkaTemplate).send("transfers", "42");
    }

    @Test
    void publish_whenKafkaIsDown_doesNotThrow() {
        when(kafkaTemplate.send(anyString(), anyString())).thenThrow(new KafkaException("Kafka is down"));

        assertDoesNotThrow(() -> transferEventPublisher.publish(42L));
    }
}
