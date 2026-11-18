package io.disputeiq.platform.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiRedactorTest {

    private final PiiRedactor redactor = new PiiRedactor();

    @Test
    void masksLuhnValidPansEmailsAndPhonesWhilePreservingNonLuhnNumbers() {
        String raw =
                "Receipt #INV-9021088412232 | Card 4111-1111-1111-1111 | "
                        + "Email alice.smith@example.com | Phone +1 (415) 555-0199 | "
                        + "AltCard 4111111111111112";

        RedactionResult result = redactor.redact(raw);

        assertThat(result.maskedPanCount()).isEqualTo(1);
        assertThat(result.piiRedactedCount()).isEqualTo(2);
        assertThat(result.totalRedactions()).isEqualTo(3);
        assertThat(result.redactedText())
                .contains("****-****-****-1111")
                .contains("[REDACTED_EMAIL]")
                .contains("[REDACTED_PHONE]")
                .contains("INV-9021088412232")
                .contains("4111111111111112")
                .doesNotContain("4111-1111-1111-1111")
                .doesNotContain("alice.smith@example.com")
                .doesNotContain("555-0199");
    }

    @Test
    void handlesBlankAndNullInputsSafely() {
        assertThat(redactor.redact(null)).isEqualTo(new RedactionResult("", 0, 0));
        assertThat(redactor.redact("   ")).isEqualTo(new RedactionResult("   ", 0, 0));
        assertThat(PiiRedactor.passesLuhn("123")).isFalse();
        assertThat(PiiRedactor.passesLuhn(null)).isFalse();
    }
}
