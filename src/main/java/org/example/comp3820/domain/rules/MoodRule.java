package org.example.comp3820.domain.rules;

import org.example.comp3820.domain.model.ClinicalFinding;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.ScoredAssessment;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Interprets the 15-item Geriatric Depression Scale.
 *
 * <p>Source: Weintraub D, Oehlberg KA, Katz IR, Stern MB. <em>Test characteristics of the
 * 15-item Geriatric Depression Scale and Hamilton Depression Rating Scale in Parkinson
 * disease.</em> Am J Geriatr Psychiatry. 2006;14(2):169–175. A cut-off of 4/5 gave 87% accuracy
 * and 88% sensitivity in Parkinson's disease, with an ROC area of 0.92.
 *
 * <p>The threshold shifts with age: 4/5 up to 75, and 5/6 above it. Age is the only reason
 * {@link PatientSnapshot} still carries a birth-derived field.
 *
 * <p>This is a <strong>screening</strong> instrument. Screening positive is not a diagnosis of
 * depression, and the wording below is careful to say so — the distinction is the difference
 * between a prompt to talk to someone and a label.
 */
public final class MoodRule {

    /** Scores at or above this screen positive. */
    static final int CUT_OFF = 5;

    /** Above this age the threshold moves up by one point. */
    static final int OLDER_AGE = 75;
    static final int OLDER_CUT_OFF = 6;

    private static final String SOURCE =
            "Weintraub et al., Am J Geriatr Psychiatry 2006 (GDS-15 validated in Parkinson's)";

    private MoodRule() {
    }

    /** Empty when the record holds no GDS-15 score. */
    public static Optional<ClinicalFinding> evaluate(PatientSnapshot snapshot) {
        List<ScoredAssessment> series = snapshot.gds15();
        if (series.isEmpty()) {
            return Optional.empty();
        }

        int cutOff = cutOffFor(snapshot.age());
        ScoredAssessment latest = series.get(series.size() - 1);

        String interpretation = latest.score() >= cutOff
                ? "at or above the screening threshold of " + cutOff + ", which is a prompt to "
                  + "discuss mood with a clinician rather than a diagnosis of depression"
                : "below the screening threshold of " + cutOff
                  + ", so this screen did not flag low mood";

        return Optional.of(new ClinicalFinding(
                "Mood (GDS-15)",
                String.format(Locale.ENGLISH, "%,.0f out of 15, recorded %s",
                        latest.score(), latest.date()),
                interpretation,
                Trends.describe(series, "points", Trends.Direction.HIGHER_IS_WORSE),
                SOURCE));
    }

    static int cutOffFor(int age) {
        return age > OLDER_AGE ? OLDER_CUT_OFF : CUT_OFF;
    }
}
