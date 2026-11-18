package io.disputeiq.platform.web;

/** Canonical HTTP and messaging correlation header names across DisputeIQ services. */
public final class CorrelationHeaders {

    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";
    public static final String REQUEST_ID = "X-Request-Id";
    public static final String TRACEPARENT = "traceparent";

    private CorrelationHeaders() {}
}
