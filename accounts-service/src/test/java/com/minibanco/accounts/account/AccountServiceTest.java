package com.minibanco.accounts.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @InjectMocks
    AccountService accountService;

    @Test
    void findById_whenAccountDoesNotExist_throwsException() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.findById(99L));
    }

    @Test
    void findById_whenAccountExists_returnsAccount() {

        // Create an account for the test
        Account account = new Account("Ana", new BigDecimal("100.00"));

        // When we look for id 1, return the account we created
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        // Look for the account with id 1 and keep the result in a variable
        AccountResponse response = accountService.findById(1L);

        // Check that the account holder is "Ana"
        assertEquals("Ana", response.holder());

    }

    @Test
    void findById_whenAccountExists_returnsBalance(){
        Account account = new Account("Ana", new BigDecimal("100.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.findById(1L);

        assertEquals(new BigDecimal("100.00"), response.balance());
    }

    @Test
    void create_savesAccountAndReturnsResponse() {
        // Create a request to open an account
        CreateAccountRequest request = new CreateAccountRequest("Ana", new BigDecimal("100.00"));

        // When an account is saved, return the same account
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Send the request and keep the response (the created account)
        AccountResponse response = accountService.create(request);

        // Check that the holder is the same
        assertEquals("Ana", response.holder());
        // Verify that the service called save()
        verify(accountRepository).save(any(Account.class));
    }
}
