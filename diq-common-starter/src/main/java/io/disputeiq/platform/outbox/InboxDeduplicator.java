package io.disputeiq.platform.outbox;

import java.time.Instant;

/** Consumer-side inbox deduplication contract for at-least-once event streams. */
public interface InboxDeduplicator {

    /**
     * Attempts to claim {@code (consumerGroup, eventId)} for processing.
     *
     * @return {@code true} if newly claimed; {@code false} if already processed.
     */
    boolean tryClaim(String consumerGroup, String eventId, Instant receivedAt);

    boolean isProcessed(String consumerGroup, String eventId);
}
