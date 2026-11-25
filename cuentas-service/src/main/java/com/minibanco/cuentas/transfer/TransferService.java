package com.minibanco.cuentas.transfer;

import com.minibanco.cuentas.account.Account;
import com.minibanco.cuentas.account.AccountNotFoundException;
import com.minibanco.cuentas.account.AccountRepository;
import com.minibanco.cuentas.commission.CommissionStrategy;
import com.minibanco.cuentas.event.TransferEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    private final List<CommissionStrategy> commissionStrategies;
    private final TransferEventPublisher transferEventPublisher;

    public TransferService(AccountRepository accountRepository, TransferRepository transferRepository, List<CommissionStrategy> commissionStrategies, TransferEventPublisher transferEventPublisher) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;

        this.commissionStrategies = commissionStrategies;
        this.transferEventPublisher = transferEventPublisher;
    }

    @Transactional
    public TransferResponse transfer(String idempotencyKey, CreateTransferRequest request) {
        // Same key as before: return the first result, don't move the money again
        Optional<Transfer> existing = transferRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return TransferResponse.from(existing.get());
        }
        Account source = accountRepository.findById(request.sourceAccountId()).orElseThrow(() -> new AccountNotFoundException(request.sourceAccountId()));

        Account target = accountRepository.findById(request.targetAccountId()).orElseThrow(() -> new AccountNotFoundException(request.targetAccountId()));

        BigDecimal commission = findStrategy(request.transferType()).calculate(request.amount());

        // The source account also pays the commission
        source.debit(request.amount().add(commission));
        target.credit(request.amount());

        Transfer transfer = new Transfer(request.sourceAccountId(), request.targetAccountId(), request.amount(), idempotencyKey, LocalDateTime.now(), "COMPLETED");

        Transfer savedTransfer;
        try {
            savedTransfer = transferRepository.save(transfer);
        } catch (DataIntegrityViolationException e) {
            // The only unique column here is idempotencyKey: another request with the same key saved it first
            throw new DuplicateIdempotencyKeyException();
        }

        TransferResponse saved = TransferResponse.from(savedTransfer);
        transferEventPublisher.publish(saved.id());
        return saved;

    }

    public TransferResponse findById(Long id) {
        Transfer transfer = transferRepository.findById(id).orElseThrow(() -> new TransferNotFoundException(id));

        return TransferResponse.from(transfer);
    }

    private CommissionStrategy findStrategy(TransferType transferType) {

        for (CommissionStrategy strategy : commissionStrategies) {
            if (strategy.supports() == transferType) {
                return strategy;
            }
        }
        throw new IllegalStateException("No commission strategy for type " + transferType);
    }
}
