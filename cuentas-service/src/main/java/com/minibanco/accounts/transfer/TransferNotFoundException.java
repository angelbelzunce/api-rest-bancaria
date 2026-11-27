package com.minibanco.accounts.transfer;

public class TransferNotFoundException extends RuntimeException {
    public TransferNotFoundException(Long transferId) {
        super("Transfer not found with id: " + transferId);
    }
}
