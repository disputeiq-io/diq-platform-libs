package io.disputeiq.platform.outbox;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe in-memory {@link InboxDeduplicator} suited for local mode and tests. */
public class InMemoryInboxDeduplicator implements InboxDeduplicator {

    private final ConcurrentHashMap<String, Instant> processed = new ConcurrentHashMap<>();

    @Override
    public boolean tryClaim(String consumerGroup, String eventId, Instant receivedAt) {
        Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        String key = compositeKey(consumerGroup, eventId);
        return processed.putIfAbsent(key, receivedAt) == null;
    }

    @Override
    public boolean isProcessed(String consumerGroup, String eventId) {
        return processed.containsKey(compositeKey(consumerGroup, eventId));
    }

    private static String compositeKey(String consumerGroup, String eventId) {
        if (consumerGroup == null || consumerGroup.isBlank()) {
            throw new IllegalArgumentException("consumerGroup must not be blank");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        return consumerGroup + ":" + eventId;
    }
}
