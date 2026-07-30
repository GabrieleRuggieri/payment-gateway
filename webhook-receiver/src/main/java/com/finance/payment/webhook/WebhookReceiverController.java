package com.finance.payment.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Controller REST di demo per ricevere e ispezionare i webhook inviati dal notification-service. */
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookReceiverController {

    private final WebhookEventStore eventStore;

    /** Accetta un payload webhook e memorizza anche gli header di firma. */
    @PostMapping("/payments")
    public ResponseEntity<Void> receive(
            @RequestBody String payload,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Webhook-Id", required = false) String webhookId,
            @RequestHeader(value = "X-Event-Type", required = false) String eventType) {
        eventStore.add(payload, signature, webhookId, eventType);
        log.info("Webhook received id={} eventType={} signaturePresent={} ({} bytes)",
                webhookId, eventType, signature != null, payload.length());
        return ResponseEntity.accepted().build();
    }

    /** Restituisce lo snapshot degli ultimi webhook ricevuti. */
    @GetMapping("/payments")
    public List<Map<String, Object>> list() {
        return eventStore.snapshot().stream()
                .map(event -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("receivedAt", event.receivedAt().toString());
                    row.put("payload", event.payload());
                    row.put("signature", event.signature());
                    row.put("webhookId", event.webhookId());
                    row.put("eventType", event.eventType());
                    return row;
                })
                .toList();
    }
}
