package io.disputeiq.platform.idempotency;

import io.disputeiq.platform.tenancy.TenantId;
import java.time.Duration;

/** Tenant-scoped store coordinating concurrent idempotent mutations and response replay. */
public interface IdempotencyStore {

    ClaimResult tryClaim(
            TenantId tenantId, IdempotencyKey key, String fingerprint, Duration leaseDuration);

    void complete(TenantId tenantId, IdempotencyKey key, IdempotencyRecord record, Duration ttl);

    void release(TenantId tenantId, IdempotencyKey key);

    /** Outcome of attempting to acquire an idempotency key for a tenant request. */
    sealed interface ClaimResult permits Claimed, Replay, InFlight, PayloadMismatch {}

    record Claimed() implements ClaimResult {}

    record Replay(IdempotencyRecord record) implements ClaimResult {}

    record InFlight() implements ClaimResult {}

    record PayloadMismatch() implements ClaimResult {}
}
