package io.disputeiq.platform.idempotency;

import java.util.Objects;

/** Validated client-supplied idempotency token scoped per tenant. */
public record IdempotencyKey(String value) {

    private static final int MAX_LENGTH = 128;

    public IdempotencyKey {
        Objects.requireNonNull(value, "IdempotencyKey value must not be null");
        value = value.strip();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "IdempotencyKey must be between 1 and " + MAX_LENGTH + " characters");
        }
    }

    public static IdempotencyKey of(String raw) {
        return new IdempotencyKey(raw);
    }

    @Override
    public String toString() {
        return value;
    }
}
