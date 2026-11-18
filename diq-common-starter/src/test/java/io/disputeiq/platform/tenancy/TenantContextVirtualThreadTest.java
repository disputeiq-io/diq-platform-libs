package io.disputeiq.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class TenantContextVirtualThreadTest {

    @Test
    void throwsWhenUnbound() {
        assertThat(TenantContext.current()).isEmpty();
        assertThatThrownBy(TenantContext::require).isInstanceOf(MissingTenantException.class);
    }

    @Test
    void isolatesTenantAcrossConcurrentVirtualThreads() throws Exception {
        int concurrency = 50;
        List<TenantId> expectedTenants = new ArrayList<>(concurrency);
        List<Future<TenantId>> futures = new ArrayList<>(concurrency);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < concurrency; i++) {
                TenantId tenant = TenantId.random();
                expectedTenants.add(tenant);
                futures.add(
                        executor.submit(
                                TenantAware.wrap(
                                        tenant,
                                        () -> {
                                            assertThat(Thread.currentThread().isVirtual()).isTrue();
                                            return TenantContext.require();
                                        })));
            }
            for (int i = 0; i < concurrency; i++) {
                assertThat(futures.get(i).get()).isEqualTo(expectedTenants.get(i));
            }
        }
        assertThat(TenantContext.current()).isEmpty();
    }

    @Test
    void recognizesGlobalTenantAndRejectsNullBindings() {
        assertThat(TenantId.GLOBAL.isGlobal()).isTrue();
        assertThat(TenantId.of(TenantId.GLOBAL.toString())).isEqualTo(TenantId.GLOBAL);
        assertThatThrownBy(() -> TenantContext.runWith(null, () -> {}))
                .isInstanceOf(MissingTenantException.class);
        assertThatThrownBy(() -> TenantContext.callWith(null, () -> "x"))
                .isInstanceOf(MissingTenantException.class);
    }
}
