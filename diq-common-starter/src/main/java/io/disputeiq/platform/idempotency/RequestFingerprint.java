package io.disputeiq.platform.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Computes canonical SHA-256 request fingerprints to detect mismatched key reuse. */
public final class RequestFingerprint {

    private RequestFingerprint() {}

    public static String sha256Hex(String method, String path, String body) {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(path, "path must not be null");
        String safeBody = body == null ? "" : body;
        String canonical = method.toUpperCase() + '\n' + path + '\n' + safeBody;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }
}
