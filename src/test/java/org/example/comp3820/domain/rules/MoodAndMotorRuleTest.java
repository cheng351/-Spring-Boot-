package org.example.comp3820.domain.rules;

import org.example.comp3820.domain.model.ClinicalFinding;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.ScoredAssessment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MoodAndMotorRuleTest {

    private static final LocalDate EARLIER = LocalDate.of(2021, 4, 1);
    private static final LocalDate LATER = LocalDate.of(2025, 3, 31);

    private PatientSnapshot snapshot(int age, List<ScoredAssessment> gds,
                                     List<ScoredAssessment> updrs) {
        return new PatientSnapshot(age, "Male", List.of("Parkinson's disease"),
                List.of(), updrs, gds);
    }

    private PatientSnapshot withGds(int age, double score) {
        return snapshot(age, List.of(new ScoredAssessment(LATER, score)), List.of());
    }

    // --- GDS-15 -----------------------------------------------------------------------------

    @Test
    void saysNothingWhenThereIsNoMoodScore() {
        assertThat(MoodRule.evaluate(snapshot(70, List.of(), List.of()))).isEmpty();
    }

    @Test
    void screensPositiveAtFiveForSomeoneUnderSeventyFive() {
        assertThat(MoodRule.evaluate(withGds(70, 5)).orElseThrow().interpretation())
                .contains("at or above the screening threshold of 5");
        assertThat(MoodRule.evaluate(withGds(70, 4)).orElseThrow().interpretation())
                .contains("below the screening threshold of 5");
    }

    @Test
    void usesTheHigherThresholdAboveSeventyFive() {
        // Weintraub et al. report 5/6 for the old-elderly group, so a 5 no longer screens
        // positive at 76 - the same number means different things at different ages.
        assertThat(MoodRule.evaluate(withGds(76, 5)).orElseThrow().interpretation())
                .contains("below the screening threshold of 6");
        assertThat(MoodRule.evaluate(withGds(76, 6)).orElseThrow().interpretation())
                .contains("at or above the screening threshold of 6");
    }

    @Test
    void thresholdChangesExactlyAfterSeventyFive() {
        assertThat(MoodRule.cutOffFor(75)).isEqualTo(5);
        assertThat(MoodRule.cutOffFor(76)).isEqualTo(6);
    }

    @Test
    void refusesToCallScreeningPositiveADiagnosis() {
        assertThat(MoodRule.evaluate(withGds(70, 12)).orElseThrow().interpretation())
                .contains("rather than a diagnosis of depression");
    }

    @Test
    void citesTheParkinsonsValidationStudy() {
        assertThat(MoodRule.evaluate(withGds(70, 12)).orElseThrow().basis())
                .contains("Weintraub");
    }

    // --- UPDRS ------------------------------------------------------------------------------

    @Test
    void saysNothingWhenThereIsNoMotorScore() {
        assertThat(MotorRule.evaluate(snapshot(70, List.of(), List.of()))).isEmpty();
    }

    @Test
    void reportsTheMotorScoreWithoutBandingIt() {
        ClinicalFinding finding = MotorRule.evaluate(snapshot(70, List.of(),
                List.of(new ScoredAssessment(LATER, 64)))).orElseThrow();

        assertThat(finding.measurement()).contains("64");
        assertThat(finding.interpretation()).contains("no severity level is given");
        // No threshold was applied, so there is no source to cite - and claiming one would be
        // worse than citing none.
        assertThat(finding.basis()).isNull();
    }

    @Test
    void treatsARisingMotorScoreAsTheUnfavourableDirection() {
        ClinicalFinding finding = MotorRule.evaluate(snapshot(70, List.of(), List.of(
                new ScoredAssessment(EARLIER, 54),
                new ScoredAssessment(LATER, 64)))).orElseThrow();

        assertThat(finding.trend()).contains("up 10 points").contains("unfavourable");
    }
}
