package io.disputeiq.platform.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.disputeiq.platform.tenancy.TenantId;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TenantRateLimiterTest {

    @Test
    void isolatesQuotasPerTenantAndResetsAfterWindow() {
        TenantRateLimiter limiter = new TenantRateLimiter();
        TenantId alpha = TenantId.random();
        TenantId beta = TenantId.random();
        Instant t0 = Instant.parse("2026-11-18T12:00:00Z");
        Duration window = Duration.ofMinutes(1);

        assertThat(limiter.tryAcquire(alpha, 2, window, t0)).isTrue();
        assertThat(limiter.tryAcquire(alpha, 2, window, t0.plusSeconds(10))).isTrue();
        assertThat(limiter.tryAcquire(alpha, 2, window, t0.plusSeconds(20))).isFalse();

        // Tenant Beta remains unaffected by Alpha exhaustion.
        assertThat(limiter.tryAcquire(beta, 2, window, t0.plusSeconds(20))).isTrue();

        // Alpha resets cleanly in next window.
        assertThat(limiter.tryAcquire(alpha, 2, window, t0.plusSeconds(60))).isTrue();
    }

    @Test
    void rejectsInvalidLimits() {
        TenantRateLimiter limiter = new TenantRateLimiter();
        TenantId tenant = TenantId.random();
        Instant now = Instant.now();
        assertThatThrownBy(() -> limiter.tryAcquire(tenant, 0, Duration.ofMinutes(1), now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limiter.tryAcquire(tenant, 5, Duration.ZERO, now))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
