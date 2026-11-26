package com.minibanco.cuentas.event;

import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
        when(kafkaTemplate.send("transfers", "42")).thenReturn(CompletableFuture.completedFuture(null));

        transferEventPublisher.publish(42L);

        verify(kafkaTemplate).send("transfers", "42");
    }

    @Test
    void publish_whenKafkaIsDown_throwsSoTheRelayRetries() {
        when(kafkaTemplate.send(anyString(), anyString())).thenThrow(new KafkaException("Kafka is down"));

        assertThrows(KafkaException.class, () -> transferEventPublisher.publish(42L));
    }
}
