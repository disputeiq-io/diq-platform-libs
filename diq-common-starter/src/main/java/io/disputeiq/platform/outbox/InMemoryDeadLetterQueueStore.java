package io.disputeiq.platform.outbox;

import io.disputeiq.platform.tenancy.TenantId;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/** Thread-safe tenant-isolated store for failed events awaiting DLQ replay. */
public class InMemoryDeadLetterQueueStore {

    private final ConcurrentHashMap<String, DeadLetterMessage> entries = new ConcurrentHashMap<>();

    public DeadLetterMessage recordFailure(
            TenantId tenantId,
            String consumerGroup,
            String originalTopic,
            String eventId,
            String payloadJson,
            String failureReason,
            Instant failedAt) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        String dlqId = UUID.randomUUID().toString();
        String dlqTopic = originalTopic.endsWith(".dlq") ? originalTopic : originalTopic + ".dlq";
        DeadLetterMessage message =
                new DeadLetterMessage(
                        dlqId,
                        tenantId,
                        consumerGroup,
                        originalTopic,
                        dlqTopic,
                        eventId,
                        payloadJson,
                        failureReason,
                        failedAt,
                        0,
                        null);
        entries.put(dlqId, message);
        return message;
    }

    public Optional<DeadLetterMessage> findById(TenantId tenantId, String dlqId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        DeadLetterMessage message = entries.get(dlqId);
        if (message == null || !message.tenantId().equals(tenantId)) {
            return Optional.empty();
        }
        return Optional.of(message);
    }

    public List<DeadLetterMessage> listForTenant(TenantId tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        return entries.values().stream()
                .filter(entry -> entry.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(DeadLetterMessage::failedAt).reversed())
                .toList();
    }

    public DeadLetterMessage markReplayed(TenantId tenantId, String dlqId, Instant replayedAt) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        AtomicReference<DeadLetterMessage> updatedRef = new AtomicReference<>();
        entries.computeIfPresent(
                dlqId,
                (key, existing) -> {
                    if (!existing.tenantId().equals(tenantId)) {
                        return existing;
                    }
                    DeadLetterMessage updated = existing.markReplayed(replayedAt);
                    updatedRef.set(updated);
                    return updated;
                });
        if (updatedRef.get() == null) {
            throw new IllegalArgumentException("DLQ entry not found for tenant: " + dlqId);
        }
        return updatedRef.get();
    }
}
