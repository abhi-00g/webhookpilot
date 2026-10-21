package com.webhookpilot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HMAC-SHA256 webhook signature generation.
 * No Spring context needed — pure logic.
 */
class HmacSignatureServiceTest {

    private HmacSignatureService hmacService;

    @BeforeEach
    void setUp() {
        hmacService = new HmacSignatureService();
    }

    @Test
    @DisplayName("sign produces correct HMAC-SHA256 hex digest")
    void sign_producesCorrectDigest() throws Exception {
        String content = "1700000000.{\"orderId\":\"123\"}";
        String secret = "whsec_test_secret_key";

        String result = hmacService.sign(content, secret);

        // Verify against Java's own HMAC implementation
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : expected) hex.append(String.format("%02x", b));

        assertEquals(hex.toString(), result);
    }

    @Test
    @DisplayName("sign produces 64-character hex string")
    void sign_produces64CharHex() {
        String result = hmacService.sign("test payload", "secret");

        assertEquals(64, result.length());
        assertTrue(result.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("sign is deterministic — same inputs produce same output")
    void sign_isDeterministic() {
        String a = hmacService.sign("payload", "secret");
        String b = hmacService.sign("payload", "secret");

        assertEquals(a, b);
    }

    @Test
    @DisplayName("different secrets produce different signatures")
    void sign_differentSecrets_differentSignatures() {
        String a = hmacService.sign("payload", "secret1");
        String b = hmacService.sign("payload", "secret2");

        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("different payloads produce different signatures")
    void sign_differentPayloads_differentSignatures() {
        String a = hmacService.sign("payload1", "secret");
        String b = hmacService.sign("payload2", "secret");

        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("timestamp prefix changes signature — prevents replay attacks")
    void sign_timestampPrefixChangesSignature() {
        String payload = "{\"orderId\":\"123\"}";
        String a = hmacService.sign("1700000000." + payload, "secret");
        String b = hmacService.sign("1700000001." + payload, "secret");

        assertNotEquals(a, b);
    }
}
