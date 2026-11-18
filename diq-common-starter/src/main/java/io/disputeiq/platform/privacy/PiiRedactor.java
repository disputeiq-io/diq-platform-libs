package io.disputeiq.platform.privacy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redacts Luhn-validated payment card PANs, email addresses, and phone numbers prior to persistence
 * or downstream event emission.
 */
public final class PiiRedactor {

    private static final Pattern PAN_CANDIDATE =
            Pattern.compile("(?<!\\d)(?:\\d[ -]?){12,18}\\d(?!\\d)");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("(?i)\\b[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile(
                    "(?<![\\d*])(?:\\+?1[-.\\s])?(?:\\(\\d{3}\\)[-.\\s]?|\\d{3}[-.\\s])\\d{3}[-.\\s]\\d{4}(?!\\d)");

    /** Applies PAN, email, and phone redaction in sequence and returns counts. */
    public RedactionResult redact(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new RedactionResult(rawText == null ? "" : rawText, 0, 0);
        }

        Counter panCounter = new Counter();
        String panMasked = redactPans(rawText, panCounter);

        Counter piiCounter = new Counter();
        String emailMasked =
                replaceMatches(panMasked, EMAIL_PATTERN, "[REDACTED_EMAIL]", piiCounter);
        String fullyMasked =
                replaceMatches(emailMasked, PHONE_PATTERN, "[REDACTED_PHONE]", piiCounter);

        return new RedactionResult(fullyMasked, panCounter.value, piiCounter.value);
    }

    private String redactPans(String text, Counter panCounter) {
        Matcher matcher = PAN_CANDIDATE.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String digits = extractDigits(matcher.group());
            if (passesLuhn(digits)) {
                panCounter.value++;
                String lastFour = digits.substring(digits.length() - 4);
                matcher.appendReplacement(
                        sb, Matcher.quoteReplacement("****-****-****-" + lastFour));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String replaceMatches(
            String text, Pattern pattern, String replacement, Counter counter) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            counter.value++;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    static String extractDigits(String input) {
        StringBuilder digits = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (Character.isDigit(ch)) {
                digits.append(ch);
            }
        }
        return digits.toString();
    }

    static boolean passesLuhn(String digits) {
        if (digits == null || digits.length() < 13 || digits.length() > 19) {
            return false;
        }
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private static final class Counter {
        private int value;
    }
}
