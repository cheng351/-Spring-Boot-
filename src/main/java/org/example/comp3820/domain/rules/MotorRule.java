package org.example.comp3820.domain.rules;

import org.example.comp3820.domain.model.ClinicalFinding;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.ScoredAssessment;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Reports the UPDRS total — and deliberately does not band it.
 *
 * <p>Two reasons, both of which would make a severity label unsupportable:
 *
 * <ol>
 *   <li>The published cut-offs (Martínez-Martín et al., Parkinsonism Relat Disord 2015) are
 *       defined <em>per MDS-UPDRS part</em> — Part 3 at 32/33 and 58/59, and so on — not for a
 *       total. The same total can arise from very different combinations of items, which is
 *       why the literature cautions against reading a total on its own.</li>
 *   <li>Those cut-offs belong to the MDS-UPDRS. The dataset records an unsplit total of an
 *       unspecified UPDRS version, and the original and MDS revisions differ in both items and
 *       range.</li>
 * </ol>
 *
 * <p>So the value and its direction of travel are reported, and nothing is claimed about
 * severity. Reporting a trajectory we can support is better than a label we cannot.
 */
public final class MotorRule {

    private MotorRule() {
    }

    /** Empty when the record holds no UPDRS score. */
    public static Optional<ClinicalFinding> evaluate(PatientSnapshot snapshot) {
        List<ScoredAssessment> series = snapshot.updrs();
        if (series.isEmpty()) {
            return Optional.empty();
        }

        ScoredAssessment latest = series.get(series.size() - 1);
        return Optional.of(new ClinicalFinding(
                "Motor symptoms (UPDRS total)",
                String.format(Locale.ENGLISH, "%,.0f, recorded %s", latest.score(), latest.date()),
                "no severity level is given, because the published thresholds apply to "
                        + "individual parts of the MDS-UPDRS rather than to a total score",
                Trends.describe(series, "points", Trends.Direction.HIGHER_IS_WORSE),
                null));
    }
}
