package io.disputeiq.platform.outbox;

import io.disputeiq.platform.tenancy.TenantId;
import java.time.Instant;
import java.util.Objects;

/** Tenant-scoped record of a failed event captured in a Dead Letter Queue for replay. */
public record DeadLetterMessage(
        String dlqId,
        TenantId tenantId,
        String consumerGroup,
        String originalTopic,
        String dlqTopic,
        String eventId,
        String payloadJson,
        String failureReason,
        Instant failedAt,
        int replayCount,
        Instant lastReplayedAt) {

    public DeadLetterMessage {
        Objects.requireNonNull(dlqId, "dlqId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(consumerGroup, "consumerGroup must not be null");
        Objects.requireNonNull(originalTopic, "originalTopic must not be null");
        Objects.requireNonNull(dlqTopic, "dlqTopic must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(payloadJson, "payloadJson must not be null");
        Objects.requireNonNull(failureReason, "failureReason must not be null");
        Objects.requireNonNull(failedAt, "failedAt must not be null");
    }

    public DeadLetterMessage markReplayed(Instant replayedAt) {
        Objects.requireNonNull(replayedAt, "replayedAt must not be null");
        return new DeadLetterMessage(
                dlqId,
                tenantId,
                consumerGroup,
                originalTopic,
                dlqTopic,
                eventId,
                payloadJson,
                failureReason,
                failedAt,
                replayCount + 1,
                replayedAt);
    }
}
