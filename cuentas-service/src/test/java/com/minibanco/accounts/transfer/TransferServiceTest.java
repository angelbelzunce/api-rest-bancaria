package com.minibanco.accounts.transfer;

import com.minibanco.accounts.account.Account;
import com.minibanco.accounts.account.AccountNotFoundException;
import com.minibanco.accounts.account.AccountRepository;
import com.minibanco.accounts.commission.InternalCommissionStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.minibanco.accounts.outbox.OutboxEventRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {

    @Mock
    TransferRepository transferRepository;

    @Mock
    AccountRepository accountRepository;

    @InjectMocks
    TransferService transferService;

    @Test
    void findById_whenTransferDoesNotExist_throwsTransferNotFoundException() {
        when(transferRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TransferNotFoundException.class, () -> transferService.findById(99L));
    }

    @Test
    void findById_whenTransferExists_returnsExistingTransfer() {
        Transfer transfer = new Transfer(1L, 2L, new BigDecimal("30.00"), "key-1", LocalDateTime.now(), "COMPLETED");

        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));

        TransferResponse response = transferService.findById(1L);

        assertEquals(new BigDecimal("30.00"), response.amount());

    }

    @Test
    void findById_whenTransferExists_returnsStatus() {
        Transfer transfer = new Transfer(1L, 2L, new BigDecimal("30.00"), "key-1", LocalDateTime.now(), "COMPLETED");

        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));

        TransferResponse response = transferService.findById(1L);

        assertEquals("COMPLETED", response.status());
    }

    @Test
    void findById_whenTransferExists_returnsSourceAccount() {
        Transfer transfer = new Transfer(1L, 2L, new BigDecimal("30.00"), "key-1", LocalDateTime.now(), "COMPLETED");

        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));

        TransferResponse response = transferService.findById(1L);

        assertEquals(1L, response.sourceAccountId());
    }

    @Test
    void findById_whenTransferExists_returnsTargetAccount(){
        Transfer transfer = new Transfer(1L, 2L, new BigDecimal("30.00"), "key-1", LocalDateTime.now(), "COMPLETED");

        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));

        TransferResponse response = transferService.findById(1L);

        assertEquals(2L, response.targetAccountId());
    }

    @Test
    void transfer_whenIdempotencyKeyWasUsed_returnsExistingTransfer() {
        Transfer transfer = new Transfer(1L, 2L, new BigDecimal("30.00"), "key-1", LocalDateTime.now(), "COMPLETED");

        when(transferRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(transfer));

        CreateTransferRequest request = new CreateTransferRequest(1L, 2L, new BigDecimal("30.00"), TransferType.INTERNAL);

        TransferResponse response = transferService.transfer("key-1", request);

        assertEquals(new BigDecimal("30.00"), response.amount());
        verify(accountRepository, never()).findById(any());

    }

    @Test
    void transfer_whenSourceAccountDoesNotExist_throwsAccountNotFoundException(){
        // Create a request
        CreateTransferRequest request = new CreateTransferRequest(1L, 2L, new BigDecimal("30.00"), TransferType.INTERNAL);

        // When the repository looks for the account with id 1, it returns an empty Optional
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // Check that the service throws the exception when it gets the request from step 1
        assertThrows(AccountNotFoundException.class, () -> transferService.transfer("key-1", request));

    }

    @Test
    void transfer_whenTargetAccountDoesNotExist_throwsAccountNotFoundException() {
        // Create a request
        CreateTransferRequest request = new CreateTransferRequest(1L, 2L, new BigDecimal("30.00"), TransferType.INTERNAL);

        // The source account is found, so the method can go on and look for the target account, which is the one we want to test
        when(accountRepository.findById(1L)).thenReturn(Optional.of(new Account("Ana", new BigDecimal("100.00"))));
        // The target account is not found (empty Optional)
        when(accountRepository.findById(2L)).thenReturn(Optional.empty());

        // Check that it throws the exception
        assertThrows(AccountNotFoundException.class, () -> transferService.transfer("key-1", request));

    }

    @Test
    void transfer_whenSaveHitsUniqueIdempotencyKey_throwsDuplicateIdempotencyKeyException() {
        // Built by hand because this test needs a real commission strategy
        TransferService service = new TransferService(accountRepository, transferRepository,
                List.of(new InternalCommissionStrategy()), mock(OutboxEventRepository.class));

        CreateTransferRequest request = new CreateTransferRequest(1L, 2L, new BigDecimal("30.00"), TransferType.INTERNAL);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(new Account("Ana", new BigDecimal("100.00"))));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(new Account("Luis", new BigDecimal("0.00"))));
        when(transferRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(DuplicateIdempotencyKeyException.class, () -> service.transfer("key-1", request));
    }

}
