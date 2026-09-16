package org.example.comp3820.domain.rules;

import org.example.comp3820.domain.model.CognitiveEngagement;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.RiskFactor;
import org.example.comp3820.domain.model.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CognitiveEngagementRuleTest {

    private static PatientSnapshot withEngagement(int done, int goal) {
        return new PatientSnapshot(68, "female", List.of("Parkinson's disease"),
                List.of(), List.of(), new CognitiveEngagement(done, goal));
    }

    @Test
    void meetingTheGoalIsOnTrack() {
        RiskFactor factor = CognitiveEngagementRule.evaluate(withEngagement(3, 3));

        assertThat(factor.level()).isEqualTo(RiskLevel.ON_TRACK);
        assertThat(factor.title()).isEqualTo("Mind & connection");
        assertThat(factor.fhirResource()).isEqualTo("CarePlan");
    }

    @Test
    void exceedingTheGoalIsStillOnTrack() {
        RiskFactor factor = CognitiveEngagementRule.evaluate(withEngagement(5, 3));

        assertThat(factor.level()).isEqualTo(RiskLevel.ON_TRACK);
    }

    @Test
    void oneShortOfTheGoalIsInProgress() {
        RiskFactor factor = CognitiveEngagementRule.evaluate(withEngagement(2, 3));

        assertThat(factor.level()).isEqualTo(RiskLevel.IN_PROGRESS);
        assertThat(factor.level().tone()).isEqualTo("blue");
        assertThat(factor.detail()).isEqualTo("2 activities this week");
        assertThat(factor.note()).isEqualTo("One more would meet your goal");
    }

    @Test
    void wellShortOfTheGoalIsWorthAttention() {
        RiskFactor factor = CognitiveEngagementRule.evaluate(withEngagement(0, 3));

        assertThat(factor.level()).isEqualTo(RiskLevel.WORTH_ATTENTION);
        assertThat(factor.note()).isEqualTo("3 more would meet your goal");
    }

    @Test
    void aSingleActivityIsDescribedInTheSingular() {
        RiskFactor factor = CognitiveEngagementRule.evaluate(withEngagement(1, 3));

        assertThat(factor.detail()).isEqualTo("1 activity this week");
    }

    @Test
    void aCarePlanWithNoGoalDoesNotJudgeThePatient() {
        RiskFactor factor = CognitiveEngagementRule.evaluate(withEngagement(0, 0));

        assertThat(factor.level()).isEqualTo(RiskLevel.IN_PROGRESS);
        assertThat(factor.detail()).isEqualTo("No goal set");
    }

    @Test
    void negativeCountsAreRejectedAtConstruction() {
        assertThatThrownBy(() -> new CognitiveEngagement(-1, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
