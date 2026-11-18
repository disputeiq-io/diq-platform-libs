package io.disputeiq.platform.outbox;

import io.disputeiq.platform.tenancy.TenantId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable domain representation of an outbox event written atomically with state changes. */
public record OutboxMessage(
        UUID eventId,
        TenantId tenantId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String topic,
        String payloadJson,
        Instant occurredAt) {

    public OutboxMessage {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        aggregateType = requireText(aggregateType, "aggregateType");
        aggregateId = requireText(aggregateId, "aggregateId");
        eventType = requireText(eventType, "eventType");
        topic = requireText(topic, "topic");
        payloadJson = requireText(payloadJson, "payloadJson");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
