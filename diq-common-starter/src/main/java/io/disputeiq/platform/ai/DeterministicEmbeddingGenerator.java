package io.disputeiq.platform.ai;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic 64-dimensional feature-hashed word and character n-gram embedding generator for
 * fast, offline-capable semantic and lexical retrieval across dispute policy chunks.
 */
public final class DeterministicEmbeddingGenerator {

    public static final int DIMENSIONS = 64;

    public double[] embed(String text) {
        double[] vector = new double[DIMENSIONS];
        if (text == null || text.isBlank()) {
            return vector;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        String[] tokens = normalized.split("[^a-z0-9.]+");
        for (String token : tokens) {
            if (token.length() >= 2) {
                projectToken(vector, token, 2.0d);
                projectTrigrams(vector, token);
            }
        }
        return l2Normalize(vector);
    }

    public static double cosineSimilarity(double[] left, double[] right) {
        if (left == null || right == null || left.length != right.length) {
            throw new IllegalArgumentException("Vectors must be non-null and equal length");
        }
        double dot = 0.0d;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
        }
        return dot;
    }

    public static double lexicalOverlapScore(String query, String document) {
        Set<String> queryTokens = tokenizeDistinct(query);
        if (queryTokens.isEmpty()) {
            return 0.0d;
        }
        Set<String> docTokens = tokenizeDistinct(document);
        int hits = 0;
        for (String q : queryTokens) {
            if (docTokens.contains(q)) {
                hits++;
            }
        }
        return (double) hits / queryTokens.size();
    }

    public static String toCsv(double[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8);
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(String.format(Locale.ROOT, "%.6f", vector[i]));
        }
        return sb.toString();
    }

    public static double[] fromCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return new double[DIMENSIONS];
        }
        String[] parts = csv.split(",");
        double[] vector = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Double.parseDouble(parts[i].strip());
        }
        return vector;
    }

    private static Set<String> tokenizeDistinct(String input) {
        Set<String> set = new HashSet<>();
        if (input == null || input.isBlank()) {
            return set;
        }
        for (String token : input.toLowerCase(Locale.ROOT).split("[^a-z0-9.]+")) {
            if (token.length() >= 2) {
                set.add(token);
            }
        }
        return set;
    }

    private static void projectTrigrams(double[] vector, String token) {
        for (int i = 0; i <= token.length() - 3; i++) {
            projectToken(vector, token.substring(i, i + 3), 0.75d);
        }
    }

    private static void projectToken(double[] vector, String feature, double weight) {
        int hash = fnv1a(feature);
        int bucket = Math.floorMod(hash, DIMENSIONS);
        double sign = ((hash & 1) == 0) ? 1.0d : -1.0d;
        vector[bucket] += sign * weight;
    }

    private static int fnv1a(String feature) {
        int hash = 0x811c9dc5;
        for (int i = 0; i < feature.length(); i++) {
            hash ^= feature.charAt(i);
            hash *= 0x01000193;
        }
        return hash;
    }

    private static double[] l2Normalize(double[] vector) {
        double sumSq = 0.0d;
        for (double v : vector) {
            sumSq += v * v;
        }
        if (sumSq == 0.0d) {
            return vector;
        }
        double norm = Math.sqrt(sumSq);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
        return vector;
    }
}
