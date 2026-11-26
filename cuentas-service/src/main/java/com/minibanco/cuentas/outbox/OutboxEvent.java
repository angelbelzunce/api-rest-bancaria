package com.minibanco.cuentas.outbox;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long transferId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(Long transferId, LocalDateTime createdAt) {
        this.transferId = transferId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTransferId() {
        return transferId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void markAsSent(LocalDateTime sentAt){
        this.sentAt = sentAt;
    }
}
