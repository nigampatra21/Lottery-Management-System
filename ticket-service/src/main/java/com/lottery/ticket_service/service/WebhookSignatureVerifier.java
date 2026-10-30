package com.lottery.ticket_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class WebhookSignatureVerifier {

    private final String secret;

    public WebhookSignatureVerifier(@Value("${razorpay.webhook-secret:}") String secret) {
        this.secret = secret;
    }

    public boolean isConfigured() {
        return !secret.isBlank();
    }

    public boolean isValid(String body, String signature) {
        if (!isConfigured() || body == null || signature == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of()
                    .formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
            // Constant-time comparison, so the signature can't be guessed from timing
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            return false;
        }
    }
}