package io.disputeiq.platform.tenancy;

import java.util.Objects;
import java.util.UUID;

/** Strongly-typed tenant identifier carried across service boundaries and virtual threads. */
public record TenantId(UUID value) {

    /** Shared read-only tenant ID representing global network rules and reference data. */
    public static final TenantId GLOBAL =
            new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

    public TenantId {
        Objects.requireNonNull(value, "TenantId value must not be null");
    }

    public static TenantId of(String raw) {
        Objects.requireNonNull(raw, "raw tenantId must not be null");
        return new TenantId(UUID.fromString(raw.strip()));
    }

    public static TenantId random() {
        return new TenantId(UUID.randomUUID());
    }

    public boolean isGlobal() {
        return GLOBAL.equals(this);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
