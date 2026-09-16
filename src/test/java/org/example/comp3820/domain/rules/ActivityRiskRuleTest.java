package org.example.comp3820.domain.rules;

import org.example.comp3820.domain.model.CognitiveEngagement;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.RiskFactor;
import org.example.comp3820.domain.model.RiskLevel;
import org.example.comp3820.domain.model.StepRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityRiskRuleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 16);

    private static PatientSnapshot withSteps(List<StepRecord> steps) {
        return new PatientSnapshot(68, "female", List.of("Parkinson's disease"),
                List.of(), steps, new CognitiveEngagement(0, 3));
    }

    private static List<StepRecord> days(int count, int steps) {
        List<StepRecord> records = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            records.add(new StepRecord(TODAY.minusDays(i), steps));
        }
        return records;
    }

    @Test
    void anActiveWeekIsOnTrack() {
        RiskFactor factor = ActivityRiskRule.evaluate(withSteps(days(7, 6000)), TODAY);

        assertThat(factor.level()).isEqualTo(RiskLevel.ON_TRACK);
        assertThat(factor.level().tone()).isEqualTo("green");
        assertThat(factor.fhirResource()).isEqualTo("Observation");
    }

    @Test
    void aMiddlingWeekIsInProgress() {
        RiskFactor factor = ActivityRiskRule.evaluate(withSteps(days(7, 4000)), TODAY);

        assertThat(factor.level()).isEqualTo(RiskLevel.IN_PROGRESS);
    }

    @Test
    void aSedentaryWeekIsWorthAttention() {
        RiskFactor factor = ActivityRiskRule.evaluate(withSteps(days(7, 1200)), TODAY);

        assertThat(factor.level()).isEqualTo(RiskLevel.WORTH_ATTENTION);
        assertThat(factor.level().tone()).isEqualTo("amber");
    }

    @Test
    void oneQuietDayDoesNotDragDownAnOtherwiseActiveWeek() {
        List<StepRecord> steps = new ArrayList<>();
        steps.add(new StepRecord(TODAY.minusDays(1), 200));
        for (int i = 2; i <= 7; i++) {
            steps.add(new StepRecord(TODAY.minusDays(i), 7000));
        }

        RiskFactor factor = ActivityRiskRule.evaluate(withSteps(steps), TODAY);

        // the average still clears the target, so the card stays positive
        assertThat(factor.level()).isEqualTo(RiskLevel.ON_TRACK);
        // ...even though the headline figure is the quiet day
        assertThat(factor.detail()).isEqualTo("200 steps yesterday");
    }

    @Test
    void detailUsesThousandsSeparatorsRegardlessOfSystemLocale() {
        RiskFactor factor = ActivityRiskRule.evaluate(
                withSteps(List.of(new StepRecord(TODAY.minusDays(1), 4280))), TODAY);

        assertThat(factor.detail()).isEqualTo("4,280 steps yesterday");
    }

    @Test
    void anOlderReadingIsDatedRatherThanCalledYesterday() {
        RiskFactor factor = ActivityRiskRule.evaluate(
                withSteps(List.of(new StepRecord(TODAY.minusDays(3), 3100))), TODAY);

        assertThat(factor.detail()).isEqualTo("3,100 steps on 2026-09-13");
    }

    @Test
    void noReadingsDegradesGracefullyRatherThanThrowing() {
        RiskFactor factor = ActivityRiskRule.evaluate(withSteps(List.of()), TODAY);

        assertThat(factor.detail()).isEqualTo("No recent readings");
        assertThat(factor.level()).isEqualTo(RiskLevel.IN_PROGRESS);
    }
}
