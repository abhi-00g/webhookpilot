package com.webhookpilot.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Computes HMAC-SHA256 signatures for webhook payload verification.
 *
 * How webhook signing works:
 * 1. We compute: HMAC-SHA256(timestamp + "." + payload, endpoint_secret)
 * 2. We send the signature in the X-Webhook-Signature header
 * 3. The subscriber computes the same HMAC on the received body using their copy of the secret
 * 4. If the signatures match, the payload is authentic and untampered
 *
 * Including the timestamp in the signed content prevents replay attacks —
 * the subscriber can reject signatures older than 5 minutes.
 */
@Service
public class HmacSignatureService {

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * Signs the given content with the secret using HMAC-SHA256.
     *
     * @param content   the string to sign (typically: timestamp + "." + payload)
     * @param secret    the endpoint's signing secret (whsec_...)
     * @return          hex-encoded HMAC digest
     */
    public String sign(String content, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256 signature", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
