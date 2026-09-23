package org.example.comp3820.domain.rules;

import org.example.comp3820.domain.model.ClinicalFinding;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.ScoredAssessment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The thresholds asserted here are the Parkinson's-specific ones from Dalrymple-Alford et al.
 * (Neurology 2010): below 26 for mild cognitive impairment, below 21 for dementia. If someone
 * changes those numbers, these tests should make them explain why.
 */
class CognitionRuleTest {

    private PatientSnapshot withMoca(double... scores) {
        List<ScoredAssessment> series = new ArrayList<>();
        LocalDate date = LocalDate.of(2021, 4, 1);
        for (double score : scores) {
            series.add(new ScoredAssessment(date, score));
            date = date.plusYears(1);
        }
        return new PatientSnapshot(70, "Female", List.of("Parkinson's disease"),
                series, List.of(), List.of());
    }

    @Test
    void saysNothingWhenTheRecordHasNoMocaScore() {
        PatientSnapshot none = new PatientSnapshot(70, "Female", List.of(),
                List.of(), List.of(), List.of());

        assertThat(CognitionRule.evaluate(none)).isEmpty();
    }

    @Test
    void doesNotFlagAScoreAtTheMciThreshold() {
        ClinicalFinding finding = CognitionRule.evaluate(withMoca(26)).orElseThrow();

        assertThat(finding.interpretation()).contains("did not flag cognitive impairment");
    }

    @Test
    void flagsTheMciBandJustBelowTheThreshold() {
        ClinicalFinding finding = CognitionRule.evaluate(withMoca(25)).orElseThrow();

        assertThat(finding.interpretation())
                .contains("mild cognitive impairment")
                .contains("above the dementia threshold");
    }

    @Test
    void flagsTheDementiaBandOnlyBelowTwentyOne() {
        assertThat(CognitionRule.evaluate(withMoca(21)).orElseThrow().interpretation())
                .contains("mild cognitive impairment");
        assertThat(CognitionRule.evaluate(withMoca(20)).orElseThrow().interpretation())
                .contains("dementia in Parkinson's disease");
    }

    @Test
    void describesThresholdsAsScreeningRatherThanDiagnosis() {
        ClinicalFinding finding = CognitionRule.evaluate(withMoca(5)).orElseThrow();

        assertThat(finding.interpretation()).contains("screening threshold");
    }

    @Test
    void reportsTheLatestScoreNotTheFirst() {
        ClinicalFinding finding = CognitionRule.evaluate(withMoca(21, 17, 5)).orElseThrow();

        assertThat(finding.measurement()).contains("5 out of 30");
    }

    @Test
    void describesAFallingScoreAsMovingTheWrongWay() {
        ClinicalFinding finding = CognitionRule.evaluate(withMoca(21, 17, 5)).orElseThrow();

        // Lower is worse on the MoCA, so a fall is unfavourable - the opposite of the UPDRS.
        assertThat(finding.trend()).contains("down 16 points").contains("unfavourable");
    }

    @Test
    void givesNoTrendFromASingleRecording() {
        assertThat(CognitionRule.evaluate(withMoca(24)).orElseThrow().trend()).isNull();
    }

    @Test
    void alwaysCitesWhereTheThresholdCameFrom() {
        assertThat(CognitionRule.evaluate(withMoca(24)).orElseThrow().basis())
                .contains("Dalrymple-Alford");
    }
}
