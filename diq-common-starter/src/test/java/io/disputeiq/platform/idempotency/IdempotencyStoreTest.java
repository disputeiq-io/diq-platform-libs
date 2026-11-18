package io.disputeiq.platform.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.disputeiq.platform.tenancy.TenantId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class IdempotencyStoreTest {

    @Test
    void enforcesClaimReplayConflictMismatchAndRelease() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC);
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(clock);
        TenantId tenant = TenantId.random();
        IdempotencyKey key = IdempotencyKey.of("idem-001");
        String fp1 = RequestFingerprint.sha256Hex("POST", "/api/v1/disputes", "{\"a\":1}");
        String fp2 = RequestFingerprint.sha256Hex("POST", "/api/v1/disputes", "{\"a\":2}");

        assertThat(store.tryClaim(tenant, key, fp1, Duration.ofSeconds(30)))
                .isInstanceOf(IdempotencyStore.Claimed.class);
        assertThat(store.tryClaim(tenant, key, fp1, Duration.ofSeconds(30)))
                .isInstanceOf(IdempotencyStore.InFlight.class);
        assertThat(store.tryClaim(tenant, key, fp2, Duration.ofSeconds(30)))
                .isInstanceOf(IdempotencyStore.PayloadMismatch.class);

        IdempotencyRecord record =
                new IdempotencyRecord(
                        201, "application/json", "{\"id\":\"123\"}", fp1, clock.instant());
        store.complete(tenant, key, record, Duration.ofHours(24));

        IdempotencyStore.ClaimResult replay =
                store.tryClaim(tenant, key, fp1, Duration.ofSeconds(30));
        assertThat(replay).isInstanceOf(IdempotencyStore.Replay.class);
        assertThat(((IdempotencyStore.Replay) replay).record().responseBody())
                .isEqualTo("{\"id\":\"123\"}");

        store.release(tenant, key);
        assertThat(store.tryClaim(tenant, key, fp1, Duration.ofSeconds(30)))
                .isInstanceOf(IdempotencyStore.Claimed.class);
    }

    @Test
    void allowsExactlyOneVirtualThreadToClaimConcurrentDuplicateKey() throws Exception {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        TenantId tenant = TenantId.random();
        IdempotencyKey key = IdempotencyKey.of("race-key-001");
        String fp = RequestFingerprint.sha256Hex("POST", "/api/v1/disputes", "{}");

        int threads = 50;
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<IdempotencyStore.ClaimResult>> futures = new ArrayList<>(threads);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threads; i++) {
                futures.add(
                        executor.submit(
                                () -> {
                                    startGate.await();
                                    return store.tryClaim(tenant, key, fp, Duration.ofSeconds(30));
                                }));
            }
            startGate.countDown();
            int claimedCount = 0;
            for (Future<IdempotencyStore.ClaimResult> future : futures) {
                if (future.get() instanceof IdempotencyStore.Claimed) {
                    claimedCount++;
                }
            }
            assertThat(claimedCount).isEqualTo(1);
        }
    }

    @Test
    void rejectsBlankOrOverlongIdempotencyKey() {
        assertThatThrownBy(() -> IdempotencyKey.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdempotencyKey.of("a".repeat(129)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
