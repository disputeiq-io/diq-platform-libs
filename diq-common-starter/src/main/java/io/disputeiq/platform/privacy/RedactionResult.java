package io.disputeiq.platform.privacy;

import java.util.Objects;

/** Result of PAN and cardholder PII redaction. */
public record RedactionResult(String redactedText, int maskedPanCount, int piiRedactedCount) {

    public RedactionResult {
        Objects.requireNonNull(redactedText, "redactedText must not be null");
    }

    public int totalRedactions() {
        return maskedPanCount + piiRedactedCount;
    }
}
