package io.disputeiq.platform.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.disputeiq.platform.tenancy.TenantId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InboxAndDlqStoreTest {

    @Test
    void inboxDeduplicatorClaimsOncePerConsumerGroupAndEventId() {
        InMemoryInboxDeduplicator deduplicator = new InMemoryInboxDeduplicator();
        Instant now = Instant.parse("2026-11-18T10:00:00Z");

        assertThat(deduplicator.tryClaim("notification-service", "evt-1", now)).isTrue();
        assertThat(deduplicator.isProcessed("notification-service", "evt-1")).isTrue();
        assertThat(deduplicator.tryClaim("notification-service", "evt-1", now)).isFalse();
        assertThat(deduplicator.tryClaim("rag-indexer", "evt-1", now)).isTrue();
        assertThatThrownBy(() -> deduplicator.tryClaim("", "evt-1", now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> deduplicator.isProcessed("notification-service", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dlqStoreEnforcesTenantIsolationAndReplayCounts() {
        InMemoryDeadLetterQueueStore store = new InMemoryDeadLetterQueueStore();
        TenantId alpha = TenantId.of("11111111-1111-1111-1111-111111111111");
        TenantId beta = TenantId.of("22222222-2222-2222-2222-222222222222");
        Instant failedAt = Instant.parse("2026-11-18T10:00:00Z");
        Instant replayedAt = Instant.parse("2026-11-18T10:05:00Z");

        DeadLetterMessage created =
                store.recordFailure(
                        alpha,
                        "notification-service",
                        "dispute.case.filed.v1",
                        "evt-9",
                        "{\"caseId\":\"case-1\"}",
                        "Simulated transient failure",
                        failedAt);

        assertThat(created.dlqTopic()).isEqualTo("dispute.case.filed.v1.dlq");
        assertThat(store.listForTenant(alpha)).hasSize(1);
        assertThat(store.listForTenant(beta)).isEmpty();
        assertThat(store.findById(beta, created.dlqId())).isEmpty();
        assertThat(store.findById(alpha, created.dlqId())).contains(created);

        DeadLetterMessage replayed = store.markReplayed(alpha, created.dlqId(), replayedAt);
        assertThat(replayed.replayCount()).isEqualTo(1);
        assertThat(replayed.lastReplayedAt()).isEqualTo(replayedAt);

        assertThatThrownBy(() -> store.markReplayed(beta, created.dlqId(), replayedAt))
                .isInstanceOf(IllegalArgumentException.class);

        OutboxMessage outbox =
                new OutboxMessage(
                        UUID.randomUUID(),
                        alpha,
                        "DISPUTE_CASE",
                        "case-1",
                        "dispute.case.filed.v1",
                        "dispute.case.filed.v1",
                        "{\"caseId\":\"case-1\"}",
                        failedAt);
        assertThat(outbox.aggregateId()).isEqualTo("case-1");
        assertThatThrownBy(
                        () ->
                                new OutboxMessage(
                                        UUID.randomUUID(),
                                        alpha,
                                        "",
                                        "case-1",
                                        "evt",
                                        "topic",
                                        "{}",
                                        failedAt))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
