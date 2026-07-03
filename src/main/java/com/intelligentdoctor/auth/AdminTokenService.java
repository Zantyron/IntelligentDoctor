package com.intelligentdoctor.auth;

import com.intelligentdoctor.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class AdminTokenService {

    private final AppProperties properties;

    public AdminTokenService(AppProperties properties) {
        this.properties = properties;
    }

    public TokenIssue issue(AdminPrincipal principal) {
        long expiresAt = Instant.now().plusSeconds(properties.getAdmin().getTokenTtlMinutes() * 60).getEpochSecond();
        String payload = principal.hospitalId() + "|" + principal.username() + "|" + principal.role() + "|" + expiresAt;
        String encodedPayload = base64Url(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return new TokenIssue(encodedPayload + "." + signature, expiresAt);
    }

    public AdminPrincipal verify(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2 || !constantTimeEquals(sign(parts[0]), parts[1])) {
            return null;
        }
        String[] fields;
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            fields = payload.split("\\|");
            if (fields.length != 4) {
                return null;
            }
        } catch (IllegalArgumentException ex) {
            return null;
        }
        long expiresAt;
        try {
            expiresAt = Long.parseLong(fields[3]);
        } catch (NumberFormatException ex) {
            return null;
        }
        if (Instant.now().getEpochSecond() > expiresAt) {
            return null;
        }
        return new AdminPrincipal(fields[0], fields[1], fields[2]);
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getAdmin().getTokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64Url(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to sign admin token", ex);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return expected != null
                && actual != null
                && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    public record TokenIssue(String token, long expiresAtEpochSeconds) {
    }
}
