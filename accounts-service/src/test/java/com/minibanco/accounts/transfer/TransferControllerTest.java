package com.minibanco.accounts.transfer;

import com.minibanco.accounts.security.JwtValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
public class TransferControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TransferService transferService;

    @MockitoBean
    JwtValidator jwtValidator;

    @Test
    void transfer_whenIdempotencyKeyIsDuplicated_returns409() throws Exception{

        when(transferService.transfer(any(), any())).thenThrow(new DuplicateIdempotencyKeyException());

        mockMvc.perform(post("/api/transfers")
                .header("Idempotency-Key", "key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A transfer with the same Idempotency-Key is already being processed"));

    }

    @Test
    void transfer_whenIdempotencyKeyIsTooLong_returns400() throws Exception {

        mockMvc.perform(post("/api/transfers")
                .header("Idempotency-Key", "k".repeat(256))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isBadRequest());

        verify(transferService, never()).transfer(any(), any());
    }

    @Test
    void transfer_whenAmountIsMissing_returns400() throws Exception {

        mockMvc.perform(post("/api/transfers")
                .header("Idempotency-Key", "key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceAccountId\":1,\"targetAccountId\":2,\"transferType\":\"INTERNAL\"}"))
                .andExpect(status().isBadRequest());
    }

    private static final String VALID_BODY = "{\"sourceAccountId\":1,\"targetAccountId\":2,\"amount\":100.00,\"transferType\":\"INTERNAL\"}";

}
