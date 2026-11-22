package io.disputeiq.platform.ai;

import io.disputeiq.platform.tenancy.TenantId;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Enforces per-tenant token quotas for LLM and agent triage executions. */
public final class TenantTokenBudget {

    private final int maxTokensPerTenant;
    private final ConcurrentHashMap<TenantId, AtomicInteger> usage = new ConcurrentHashMap<>();

    public TenantTokenBudget(int maxTokensPerTenant) {
        if (maxTokensPerTenant <= 0) {
            throw new IllegalArgumentException("maxTokensPerTenant must be positive");
        }
        this.maxTokensPerTenant = maxTokensPerTenant;
    }

    public boolean tryConsume(TenantId tenantId, int tokens) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        if (tokens <= 0) {
            throw new IllegalArgumentException("tokens must be positive");
        }
        AtomicInteger counter = usage.computeIfAbsent(tenantId, ignored -> new AtomicInteger());
        while (true) {
            int current = counter.get();
            if (current + tokens > maxTokensPerTenant) {
                return false;
            }
            if (counter.compareAndSet(current, current + tokens)) {
                return true;
            }
        }
    }

    public int consumed(TenantId tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        AtomicInteger counter = usage.get(tenantId);
        return counter == null ? 0 : counter.get();
    }
}
