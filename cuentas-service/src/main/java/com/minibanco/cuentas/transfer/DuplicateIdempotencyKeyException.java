package com.minibanco.cuentas.transfer;

public class DuplicateIdempotencyKeyException extends RuntimeException {
    public DuplicateIdempotencyKeyException() {
        super("A transfer with the same Idempotency-Key is already being processed");
    }
}
