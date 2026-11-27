package com.minibanco.accounts.commission;

import com.minibanco.accounts.transfer.TransferType;

import java.math.BigDecimal;

// One implementation per TransferType. TransferService picks the one that supports() the type
public interface CommissionStrategy {

    BigDecimal calculate(BigDecimal amount);

    TransferType supports();
}
