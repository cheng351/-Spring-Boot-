package org.example.comp3820.domain.rules;

import org.example.comp3820.domain.model.CognitiveEngagement;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.RiskFactor;
import org.example.comp3820.domain.model.RiskLevel;
import org.example.comp3820.domain.model.SleepRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SleepRiskRuleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 16);

    private static PatientSnapshot withSleep(List<SleepRecord> sleep) {
        return new PatientSnapshot(68, "female", List.of("Parkinson's disease"),
                sleep, List.of(), new CognitiveEngagement(0, 3));
    }

    /** Builds n consecutive nights ending yesterday, each of the given duration. */
    private static List<SleepRecord> nights(int count, double hours) {
        List<SleepRecord> records = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            records.add(new SleepRecord(TODAY.minusDays(i), hours));
        }
        return records;
    }

    @Test
    void sleepingWellAllWeekIsOnTrack() {
        RiskFactor factor = SleepRiskRule.evaluate(withSleep(nights(7, 8.0)), TODAY);

        assertThat(factor.level()).isEqualTo(RiskLevel.ON_TRACK);
        assertThat(factor.title()).isEqualTo("Sleep");
        assertThat(factor.fhirResource()).isEqualTo("Observation");
    }

    @Test
    void threeShortNightsRaisesTheCardToAttention() {
        List<SleepRecord> sleep = new ArrayList<>(nights(4, 8.0));
        // three short nights, further back than the good ones
        for (int i = 5; i <= 7; i++) {
            sleep.add(new SleepRecord(TODAY.minusDays(i), 5.5));
        }

        RiskFactor factor = SleepRiskRule.evaluate(withSleep(sleep), TODAY);

        assertThat(factor.level()).isEqualTo(RiskLevel.WORTH_ATTENTION);
        assertThat(factor.level().tone()).isEqualTo("amber");
        assertThat(factor.level().status()).isEqualTo("Worth attention");
    }

    @Test
    void twoShortNightsIsNotYetAConcern() {
        List<SleepRecord> sleep = new ArrayList<>(nights(5, 8.0));
        sleep.add(new SleepRecord(TODAY.minusDays(6), 6.0));
        sleep.add(new SleepRecord(TODAY.minusDays(7), 6.0));

        RiskFactor factor = SleepRiskRule.evaluate(withSleep(sleep), TODAY);

        assertThat(factor.level()).isEqualTo(RiskLevel.IN_PROGRESS);
    }

    @Test
    void detailReportsTheMostRecentNightInHoursAndMinutes() {
        List<SleepRecord> sleep = List.of(
                new SleepRecord(TODAY.minusDays(1), 6.1667),
                new SleepRecord(TODAY.minusDays(2), 8.0));

        RiskFactor factor = SleepRiskRule.evaluate(withSleep(sleep), TODAY);

        assertThat(factor.detail()).isEqualTo("6 h 10 min last night");
    }

    @Test
    void wholeHoursOmitTheMinutes() {
        RiskFactor factor = SleepRiskRule.evaluate(
                withSleep(List.of(new SleepRecord(TODAY.minusDays(1), 7.0))), TODAY);

        assertThat(factor.detail()).isEqualTo("7 h last night");
    }

    @Test
    void anOlderReadingIsDatedRatherThanCalledLastNight() {
        RiskFactor factor = SleepRiskRule.evaluate(
                withSleep(List.of(new SleepRecord(TODAY.minusDays(4), 7.5))), TODAY);

        assertThat(factor.detail()).isEqualTo("7 h 30 min on 2026-09-12");
    }

    @Test
    void nightsOlderThanTheWindowAreIgnored() {
        List<SleepRecord> sleep = new ArrayList<>(nights(7, 8.0));
        // three short nights from well over a week ago must not drag the card down
        for (int i = 20; i <= 22; i++) {
            sleep.add(new SleepRecord(TODAY.minusDays(i), 4.0));
        }

        RiskFactor factor = SleepRiskRule.evaluate(withSleep(sleep), TODAY);

        assertThat(factor.level()).isEqualTo(RiskLevel.ON_TRACK);
    }

    @Test
    void noReadingsDegradesGracefullyRatherThanThrowing() {
        RiskFactor factor = SleepRiskRule.evaluate(withSleep(List.of()), TODAY);

        assertThat(factor.detail()).isEqualTo("No recent readings");
        assertThat(factor.level()).isEqualTo(RiskLevel.IN_PROGRESS);
    }
}
