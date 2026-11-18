package io.disputeiq.platform.tenancy;

import java.util.Objects;
import java.util.concurrent.Callable;

/** Helpers for propagating {@link TenantContext} explicitly to child virtual-thread tasks. */
public final class TenantAware {

    private TenantAware() {}

    public static <T> Callable<T> wrap(TenantId tenantId, Callable<T> task) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(task, "task must not be null");
        return () -> TenantContext.callWith(tenantId, task);
    }

    public static Runnable wrap(TenantId tenantId, Runnable task) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(task, "task must not be null");
        return () -> TenantContext.runWith(tenantId, task);
    }
}
