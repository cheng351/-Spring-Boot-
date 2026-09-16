package org.example.comp3820.domain.rules;

import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.RiskFactor;
import org.example.comp3820.domain.model.RiskLevel;
import org.example.comp3820.domain.model.StepRecord;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Turns recent step counts into the deterministic half of the Movement card.
 *
 * <p>The card reports the most recent day's figure but grades on the weekly average, so a
 * single quiet day does not push an otherwise active patient into "Worth attention".
 *
 * <p><strong>Thresholds are provisional.</strong> General-population step targets are a poor
 * fit for people with Parkinson's, whose mobility varies with medication state and disease
 * stage, so {@link #ON_TRACK_DAILY_STEPS} and {@link #ATTENTION_DAILY_STEPS} are deliberately
 * conservative placeholders. They must be confirmed with the project mentor and cited in the
 * report before submission.
 */
public final class ActivityRiskRule {

    /** Weekly average at or above this reads as "On track". */
    public static final int ON_TRACK_DAILY_STEPS = 5000;

    /** Weekly average below this reads as "Worth attention". */
    public static final int ATTENTION_DAILY_STEPS = 3000;

    /** How many days of history the average is taken over. */
    public static final int WINDOW_DAYS = 7;

    private ActivityRiskRule() {
    }

    /**
     * @param snapshot the patient's recent data
     * @param today    the date to interpret "yesterday" against, passed in so the rule stays
     *                 deterministic under test rather than reading the system clock
     */
    public static RiskFactor evaluate(PatientSnapshot snapshot, LocalDate today) {
        List<StepRecord> window = snapshot.steps().stream()
                .sorted(Comparator.comparing(StepRecord::date).reversed())
                .limit(WINDOW_DAYS)
                .toList();

        if (window.isEmpty()) {
            return new RiskFactor("Movement", "Observation", "No recent readings",
                    "We will show this once movement is recorded", RiskLevel.IN_PROGRESS);
        }

        StepRecord latest = window.get(0);
        double average = window.stream().mapToInt(StepRecord::steps).average().orElse(0);

        RiskLevel level;
        String note;
        if (average >= ON_TRACK_DAILY_STEPS) {
            level = RiskLevel.ON_TRACK;
            note = "You are building momentum";
        } else if (average >= ATTENTION_DAILY_STEPS) {
            level = RiskLevel.IN_PROGRESS;
            note = "A little more would help";
        } else {
            level = RiskLevel.WORTH_ATTENTION;
            note = "Gentle movement could help";
        }

        return new RiskFactor("Movement", "Observation", describe(latest, today), note, level);
    }

    private static String describe(StepRecord record, LocalDate today) {
        String count = String.format(Locale.ENGLISH, "%,d steps", record.steps());
        String when = record.date().equals(today.minusDays(1))
                ? "yesterday"
                : "on " + record.date();
        return count + " " + when;
    }
}
