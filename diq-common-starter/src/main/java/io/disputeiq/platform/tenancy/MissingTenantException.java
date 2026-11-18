package io.disputeiq.platform.tenancy;

/** Thrown when an operation requiring tenant isolation is invoked without a bound tenant. */
public class MissingTenantException extends RuntimeException {

    public MissingTenantException() {
        super("No TenantId is bound to the current execution scope");
    }

    public MissingTenantException(String message) {
        super(message);
    }
}
