package io.disputeiq.platform.ai;

import io.disputeiq.platform.privacy.PiiRedactor;
import io.disputeiq.platform.privacy.RedactionResult;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies that every inline {@code [RULE-...]} citation in an AI draft exists in the retrieved
 * knowledge context and that the output contains zero unmasked PANs or PII tokens.
 */
public final class GroundednessJudge {

    private static final Pattern CITATION_TAG = Pattern.compile("\\[([A-Z]+-[A-Z0-9._-]{3,36})\\]");

    private final PiiRedactor piiRedactor;

    public GroundednessJudge(PiiRedactor piiRedactor) {
        this.piiRedactor = Objects.requireNonNull(piiRedactor, "piiRedactor must not be null");
    }

    public Judgment evaluate(String draftText, Set<String> retrievedRuleCodes) {
        Objects.requireNonNull(draftText, "draftText must not be null");
        Objects.requireNonNull(retrievedRuleCodes, "retrievedRuleCodes must not be null");

        RedactionResult egressScan = piiRedactor.redact(draftText);
        boolean piiClean = egressScan.totalRedactions() == 0;

        Set<String> cited = extractCitations(draftText);
        List<String> verified = new ArrayList<>();
        List<String> hallucinated = new ArrayList<>();
        for (String code : cited) {
            if (retrievedRuleCodes.contains(code)) {
                verified.add(code);
            } else {
                hallucinated.add(code);
            }
        }

        double citationPrecision = cited.isEmpty() ? 0.0d : (double) verified.size() / cited.size();
        double groundednessScore =
                (!cited.isEmpty() && hallucinated.isEmpty() && piiClean)
                        ? 1.0d
                        : citationPrecision * (piiClean ? 0.8d : 0.0d);
        boolean passed = !verified.isEmpty() && hallucinated.isEmpty() && piiClean;

        return new Judgment(
                passed,
                citationPrecision,
                groundednessScore,
                List.copyOf(verified),
                List.copyOf(hallucinated),
                piiClean);
    }

    private static Set<String> extractCitations(String text) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = CITATION_TAG.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    public record Judgment(
            boolean passed,
            double citationPrecision,
            double groundednessScore,
            List<String> verifiedCitations,
            List<String> hallucinatedCitations,
            boolean piiClean) {}
}
