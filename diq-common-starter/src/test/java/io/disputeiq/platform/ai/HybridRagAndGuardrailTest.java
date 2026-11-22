package io.disputeiq.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.disputeiq.platform.privacy.PiiRedactor;
import io.disputeiq.platform.tenancy.TenantId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HybridRagAndGuardrailTest {

    @Test
    void reciprocalRankFusionMergesDenseAndSparseRankingsDeterministically() {
        List<String> dense = List.of("RULE-10.4", "RULE-13.1", "RULE-12.6");
        List<String> sparse = List.of("RULE-13.1", "RULE-10.4", "RULE-13.7");

        List<ReciprocalRankFusion.RankedItem<String>> fused =
                ReciprocalRankFusion.fuse(
                        dense, sparse, item -> item, ReciprocalRankFusion.DEFAULT_K, 3);

        assertThat(fused).hasSize(3);
        assertThat(fused.get(0).id()).isEqualTo("RULE-10.4");
        assertThat(fused.get(1).id()).isEqualTo("RULE-13.1");
        assertThat(fused.get(0).rrfScore()).isEqualTo(fused.get(1).rrfScore());
        assertThatThrownBy(() -> ReciprocalRankFusion.fuse(dense, sparse, item -> item, 0, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deterministicEmbeddingsAndLexicalOverlapRankMatchingRulesHigher() {
        DeterministicEmbeddingGenerator generator = new DeterministicEmbeddingGenerator();
        double[] queryVec =
                generator.embed("Compelling Evidence 3.0 device fingerprint IP address fraud 10.4");
        double[] matchingVec =
                generator.embed(
                        "Reason Code 10.4 Compelling Evidence 3.0 requires two prior undisputed transactions matching IP address and device fingerprint");
        double[] unrelatedVec =
                generator.embed(
                        "Hotel banquet cancellation policy and airline baggage refund voucher");

        double matchSim = DeterministicEmbeddingGenerator.cosineSimilarity(queryVec, matchingVec);
        double unrelatedSim =
                DeterministicEmbeddingGenerator.cosineSimilarity(queryVec, unrelatedVec);
        assertThat(matchSim).isGreaterThan(unrelatedSim);

        String csv = DeterministicEmbeddingGenerator.toCsv(queryVec);
        double[] roundTrip = DeterministicEmbeddingGenerator.fromCsv(csv);
        assertThat(roundTrip).hasSize(DeterministicEmbeddingGenerator.DIMENSIONS);
        assertThat(DeterministicEmbeddingGenerator.fromCsv(" "))
                .hasSize(DeterministicEmbeddingGenerator.DIMENSIONS);

        double lexical =
                DeterministicEmbeddingGenerator.lexicalOverlapScore(
                        "10.4 compelling evidence device",
                        "Rule 10.4 compelling evidence device match");
        assertThat(lexical).isEqualTo(1.0d);
    }

    @Test
    void groundednessJudgeAndTenantTokenBudgetEnforceResponsibleAiControls() {
        GroundednessJudge judge = new GroundednessJudge(new PiiRedactor());
        Set<String> allowed = Set.of("RULE-VISA-10.4-CE30", "RULE-VISA-13.1-POD");

        GroundednessJudge.Judgment valid =
                judge.evaluate(
                        "Representment valid under [RULE-VISA-10.4-CE30] with masked card ****-****-****-1111.",
                        allowed);
        assertThat(valid.passed()).isTrue();
        assertThat(valid.citationPrecision()).isEqualTo(1.0d);
        assertThat(valid.groundednessScore()).isEqualTo(1.0d);
        assertThat(valid.piiClean()).isTrue();

        GroundednessJudge.Judgment hallucinated =
                judge.evaluate("Representment valid under [RULE-FAKE-99.9].", allowed);
        assertThat(hallucinated.passed()).isFalse();
        assertThat(hallucinated.hallucinatedCitations()).containsExactly("RULE-FAKE-99.9");

        GroundednessJudge.Judgment leakedPan =
                judge.evaluate(
                        "Representment under [RULE-VISA-10.4-CE30] for card 4111-1111-1111-1111.",
                        allowed);
        assertThat(leakedPan.passed()).isFalse();
        assertThat(leakedPan.piiClean()).isFalse();

        TenantTokenBudget budget = new TenantTokenBudget(1000);
        TenantId alpha = TenantId.random();
        TenantId beta = TenantId.random();
        assertThat(budget.tryConsume(alpha, 600)).isTrue();
        assertThat(budget.consumed(alpha)).isEqualTo(600);
        assertThat(budget.tryConsume(alpha, 500)).isFalse();
        assertThat(budget.tryConsume(beta, 500)).isTrue();
    }
}
