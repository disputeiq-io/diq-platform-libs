package io.disputeiq.platform.idempotency;

import java.time.Instant;
import java.util.Objects;

/** Stored response snapshot associated with a completed idempotent mutation. */
public record IdempotencyRecord(
        int statusCode,
        String contentType,
        String responseBody,
        String fingerprint,
        Instant createdAt) {

    public IdempotencyRecord {
        Objects.requireNonNull(contentType, "contentType must not be null");
        Objects.requireNonNull(responseBody, "responseBody must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
