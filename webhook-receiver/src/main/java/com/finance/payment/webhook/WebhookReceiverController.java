package com.finance.payment.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookReceiverController {

    private final WebhookEventStore eventStore;

    @PostMapping("/payments")
    public ResponseEntity<Void> receive(@RequestBody String payload) {
        eventStore.add(payload);
        log.info("Webhook received ({} bytes)", payload.length());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/payments")
    public List<Map<String, Object>> list() {
        return eventStore.snapshot().stream()
                .map(event -> Map.<String, Object>of(
                        "receivedAt", event.receivedAt().toString(),
                        "payload", event.payload()
                ))
                .toList();
    }
}
