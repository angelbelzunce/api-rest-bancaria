package com.minibanco.notificaciones.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
public class NotificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NotificationService notificationService;

    @Test
    void findAll_returnsNotifications() throws Exception {
        when(notificationService.findAll()).thenReturn(List.of(new Notification(1L, "Transfer completed")));

        mockMvc.perform(get("/api/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transferId").value(1))
                .andExpect(jsonPath("$[0].message").value("Transfer completed"));
    }

    @Test
    void create_returns201() throws Exception {
        when(notificationService.save(any(), any())).thenReturn(new Notification(1L, "Transfer completed"));

        mockMvc.perform(post("/api/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transferId\":1,\"message\":\"Transfer completed\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transferId").value(1));
    }
}
