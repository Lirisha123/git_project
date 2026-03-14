package com.insight.dashboard.security;

import com.insight.dashboard.exception.AppException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final byte[] secret;
    private final long expirationSeconds;

    public TokenService(@Value("${app.auth.token-secret:replace-me-with-env-secret}") String secret,
                        @Value("${app.auth.token-expiration-seconds:86400}") long expirationSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(Long userId, String email) {
        long expiresAt = Instant.now().getEpochSecond() + expirationSeconds;
        String payload = userId + ":" + email + ":" + expiresAt;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
            + "." + sign(payload);
    }

    public AuthPrincipal parseToken(String token) {
        if (token == null || token.isBlank() || !token.contains(".")) {
            throw new AppException("Invalid authentication token.");
        }

        String[] parts = token.split("\\.", 2);
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(parts[1].getBytes(StandardCharsets.UTF_8), sign(payload).getBytes(StandardCharsets.UTF_8))) {
            throw new AppException("Invalid authentication token signature.");
        }

        String[] values = payload.split(":", 3);
        if (values.length != 3) {
            throw new AppException("Invalid authentication token payload.");
        }

        long expiresAt = Long.parseLong(values[2]);
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new AppException("Authentication token expired.");
        }

        return new AuthPrincipal(Long.parseLong(values[0]), values[1]);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AppException("Unable to generate authentication token.", exception);
        }
    }
}
