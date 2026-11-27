package com.minibanco.accounts.account;

import com.minibanco.accounts.security.JwtValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
public class AccountControllerTest {


    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AccountService accountService;

    @MockitoBean
    JwtValidator jwtValidator;

    @Test
    void findById_whenAccountDoesNotExist_returns404() throws Exception {

        // When we look for the account with id 99, the service throws the exception
        when(accountService.findById(99L)).thenThrow(new AccountNotFoundException(99L));


        mockMvc.perform(get("/api/accounts/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_whenHolderIsBlank_returns400() throws Exception {

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"holder\":\"\",\"initialBalance\":100.00}"))
                .andExpect(status().isBadRequest());

    }

    @Test
    void create_whenHolderIsTooLong_returns400() throws Exception {

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"holder\":\"" + "a".repeat(256) + "\",\"initialBalance\":100.00}"))
                .andExpect(status().isBadRequest());

    }

}
