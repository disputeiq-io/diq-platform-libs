package io.disputeiq.platform.idempotency;

import io.disputeiq.platform.tenancy.TenantId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe, atomic {@link IdempotencyStore} backed by {@link ConcurrentHashMap} for local dev
 * and deterministic virtual-thread concurrency tests.
 */
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final ConcurrentHashMap<CompositeKey, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryIdempotencyStore() {
        this(Clock.systemUTC());
    }

    public InMemoryIdempotencyStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ClaimResult tryClaim(
            TenantId tenantId, IdempotencyKey key, String fingerprint, Duration leaseDuration) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(leaseDuration, "leaseDuration must not be null");

        CompositeKey compositeKey = new CompositeKey(tenantId, key.value());
        Instant now = clock.instant();
        AtomicReference<ClaimResult> outcome = new AtomicReference<>();

        entries.compute(
                compositeKey,
                (ignored, existing) -> {
                    if (existing == null || existing.expiresAt().isBefore(now)) {
                        outcome.set(new Claimed());
                        return new Entry(fingerprint, null, now.plus(leaseDuration));
                    }
                    if (!existing.fingerprint().equals(fingerprint)) {
                        outcome.set(new PayloadMismatch());
                        return existing;
                    }
                    if (existing.record() != null) {
                        outcome.set(new Replay(existing.record()));
                        return existing;
                    }
                    outcome.set(new InFlight());
                    return existing;
                });

        return outcome.get();
    }

    @Override
    public void complete(
            TenantId tenantId, IdempotencyKey key, IdempotencyRecord record, Duration ttl) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");

        CompositeKey compositeKey = new CompositeKey(tenantId, key.value());
        Instant now = clock.instant();
        entries.put(compositeKey, new Entry(record.fingerprint(), record, now.plus(ttl)));
    }

    @Override
    public void release(TenantId tenantId, IdempotencyKey key) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        entries.remove(new CompositeKey(tenantId, key.value()));
    }

    private record CompositeKey(TenantId tenantId, String key) {}

    private record Entry(String fingerprint, IdempotencyRecord record, Instant expiresAt) {}
}
