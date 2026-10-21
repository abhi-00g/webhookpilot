package com.webhookpilot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/test/receive")
public class TestReceiverController {

    private static final Logger log = LoggerFactory.getLogger(TestReceiverController.class);

    /**
     * Always returns 200 — demonstrates successful delivery.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> receiveSuccess(@RequestBody String payload,
                                                               @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        log.info("Test receiver (success): received payload, signature={}", signature != null ? signature.substring(0, Math.min(signature.length(), 12)) + "..." : "none");
        return ResponseEntity.ok(Map.of("status", "received", "endpoint", "success"));
    }

    /**
     * Randomly returns 200 or 500 — demonstrates retries with partial success.
     * ~50% failure rate so the demo shows a mix of outcomes.
     */
    @PostMapping("/flaky")
    public ResponseEntity<Map<String, String>> receiveFlaky(@RequestBody String payload,
                                                             @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        boolean shouldFail = ThreadLocalRandom.current().nextBoolean();
        if (shouldFail) {
            log.warn("Test receiver (flaky): simulating failure");
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "endpoint", "flaky", "message", "Simulated transient failure"));
        }
        log.info("Test receiver (flaky): simulating success");
        return ResponseEntity.ok(Map.of("status", "received", "endpoint", "flaky"));
    }

    /**
     * Always returns 500 — demonstrates FAILED → retries → EXHAUSTED → DLQ.
     */
    @PostMapping("/fail")
    public ResponseEntity<Map<String, String>> receiveFail(@RequestBody String payload,
                                                            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        log.warn("Test receiver (fail): returning 500");
        return ResponseEntity.internalServerError().body(Map.of("status", "error", "endpoint", "fail", "message", "Endpoint permanently unavailable"));
    }
}