package org.example.comp3820.domain.rules;

import org.example.comp3820.domain.model.CognitiveEngagement;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.RiskFactor;
import org.example.comp3820.domain.model.RiskLevel;

/**
 * Turns care-plan progress into the deterministic half of the Mind &amp; connection card.
 *
 * <p>Unlike sleep and movement there is no measured quantity here, so this rule grades the
 * patient against their own care plan rather than a population threshold — which sidesteps
 * the fact that no LOINC code represents cognitive engagement as an activity count.
 */
public final class CognitiveEngagementRule {

    private CognitiveEngagementRule() {
    }

    public static RiskFactor evaluate(PatientSnapshot snapshot) {
        CognitiveEngagement engagement = snapshot.cognitive();

        if (engagement.weeklyGoal() == 0) {
            return new RiskFactor("Mind & connection", "CarePlan", "No goal set",
                    "Your care plan has no activity goal yet", RiskLevel.IN_PROGRESS);
        }

        int remaining = engagement.remaining();

        RiskLevel level;
        String note;
        if (remaining == 0) {
            level = RiskLevel.ON_TRACK;
            note = "You have met your goal this week";
        } else if (remaining == 1) {
            level = RiskLevel.IN_PROGRESS;
            note = "One more would meet your goal";
        } else {
            level = RiskLevel.WORTH_ATTENTION;
            note = remaining + " more would meet your goal";
        }

        int done = engagement.completedThisWeek();
        String detail = done + (done == 1 ? " activity" : " activities") + " this week";

        return new RiskFactor("Mind & connection", "CarePlan", detail, note, level);
    }
}
