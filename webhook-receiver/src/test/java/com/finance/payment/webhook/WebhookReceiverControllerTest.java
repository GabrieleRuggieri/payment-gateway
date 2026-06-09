package com.finance.payment.webhook;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica che {@link WebhookReceiverController} accetti webhook via POST e li esponga
 * nell'endpoint GET per ispezione dei payload ricevuti.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebhookReceiverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAcceptWebhookAndExposeItOnGet() throws Exception {
        mockMvc.perform(post("/webhooks/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"PaymentSettled\"}"))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/webhooks/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payload").value("{\"eventType\":\"PaymentSettled\"}"));
    }
}
