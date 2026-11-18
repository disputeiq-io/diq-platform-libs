package io.disputeiq.platform.ratelimit;

import io.disputeiq.platform.tenancy.TenantId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe per-tenant fixed-window rate limiter preventing noisy-neighbor saturation. */
public class TenantRateLimiter {

    private final ConcurrentHashMap<TenantId, WindowBucket> buckets = new ConcurrentHashMap<>();

    public boolean tryAcquire(
            TenantId tenantId, int maxRequestsPerWindow, Duration window, Instant now) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(window, "window must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (maxRequestsPerWindow <= 0) {
            throw new IllegalArgumentException("maxRequestsPerWindow must be positive");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }

        WindowBucket updated =
                buckets.compute(
                        tenantId,
                        (ignored, current) -> {
                            if (current == null
                                    || !now.isBefore(current.windowStart().plus(window))) {
                                return new WindowBucket(now, 1);
                            }
                            return new WindowBucket(current.windowStart(), current.count() + 1);
                        });
        return updated.count() <= maxRequestsPerWindow;
    }

    private record WindowBucket(Instant windowStart, int count) {}
}
