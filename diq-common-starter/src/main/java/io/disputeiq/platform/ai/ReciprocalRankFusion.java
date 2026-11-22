package io.disputeiq.platform.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Combines dense vector similarity rankings and sparse lexical (BM25/FTS) rankings using Reciprocal
 * Rank Fusion: {@code RRF(d) = sum(1 / (k + rank_m(d)))}.
 */
public final class ReciprocalRankFusion {

    public static final int DEFAULT_K = 60;

    private ReciprocalRankFusion() {}

    public static <T> List<RankedItem<T>> fuse(
            List<T> denseRanked,
            List<T> sparseRanked,
            Function<T, String> idExtractor,
            int k,
            int topK) {
        Objects.requireNonNull(denseRanked, "denseRanked must not be null");
        Objects.requireNonNull(sparseRanked, "sparseRanked must not be null");
        Objects.requireNonNull(idExtractor, "idExtractor must not be null");
        if (k <= 0 || topK <= 0) {
            throw new IllegalArgumentException("k and topK must be positive");
        }

        Map<String, Accumulator<T>> accumulators = new LinkedHashMap<>();
        accumulateList(denseRanked, idExtractor, k, true, accumulators);
        accumulateList(sparseRanked, idExtractor, k, false, accumulators);

        List<RankedItem<T>> fused = new ArrayList<>(accumulators.size());
        for (Map.Entry<String, Accumulator<T>> entry : accumulators.entrySet()) {
            Accumulator<T> acc = entry.getValue();
            fused.add(
                    new RankedItem<>(
                            acc.item, entry.getKey(), acc.score, acc.denseRank, acc.sparseRank));
        }

        fused.sort(
                Comparator.comparingDouble(RankedItem<T>::rrfScore)
                        .reversed()
                        .thenComparingInt(
                                item ->
                                        item.denseRank() == 0
                                                ? Integer.MAX_VALUE
                                                : item.denseRank())
                        .thenComparing(RankedItem::id));

        return fused.size() <= topK ? List.copyOf(fused) : List.copyOf(fused.subList(0, topK));
    }

    private static <T> void accumulateList(
            List<T> ranked,
            Function<T, String> idExtractor,
            int k,
            boolean dense,
            Map<String, Accumulator<T>> target) {
        for (int i = 0; i < ranked.size(); i++) {
            T item = ranked.get(i);
            String id = idExtractor.apply(item);
            int rank = i + 1;
            Accumulator<T> acc = target.computeIfAbsent(id, ignored -> new Accumulator<>(item));
            acc.score += 1.0d / (k + rank);
            if (dense && acc.denseRank == 0) {
                acc.denseRank = rank;
            } else if (!dense && acc.sparseRank == 0) {
                acc.sparseRank = rank;
            }
        }
    }

    public record RankedItem<T>(
            T item, String id, double rrfScore, int denseRank, int sparseRank) {}

    private static final class Accumulator<T> {
        private final T item;
        private double score;
        private int denseRank;
        private int sparseRank;

        private Accumulator(T item) {
            this.item = item;
        }
    }
}
