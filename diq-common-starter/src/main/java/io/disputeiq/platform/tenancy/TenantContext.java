package io.disputeiq.platform.tenancy;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Virtual-thread-safe tenant scope backed by JDK {@link ScopedValue}.
 *
 * <p>Unlike {@code ThreadLocal}, scoped values are immutable for the dynamic lifetime of a task and
 * cannot leak across pooled threads.
 */
public final class TenantContext {

    public static final ScopedValue<TenantId> CURRENT = ScopedValue.newInstance();

    private TenantContext() {}

    public static TenantId require() {
        if (!CURRENT.isBound()) {
            throw new MissingTenantException();
        }
        return CURRENT.get();
    }

    public static Optional<TenantId> current() {
        return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
    }

    public static void runWith(TenantId tenantId, Runnable action) {
        if (tenantId == null) {
            throw new MissingTenantException("Cannot bind null TenantId");
        }
        ScopedValue.where(CURRENT, tenantId).run(action);
    }

    public static <T> T callWith(TenantId tenantId, Callable<T> action) throws Exception {
        if (tenantId == null) {
            throw new MissingTenantException("Cannot bind null TenantId");
        }
        return ScopedValue.where(CURRENT, tenantId).call(action);
    }
}
