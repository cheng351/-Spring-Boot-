package org.example.comp3820.domain.rules;

import org.example.comp3820.domain.model.ClinicalFinding;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.ScoredAssessment;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Interprets the Montreal Cognitive Assessment.
 *
 * <p>The cut-offs are the Parkinson's-specific ones. This matters: the familiar MoCA threshold
 * of 26 comes from Alzheimer's screening, and the literature is explicit that the general
 * cut-offs are not appropriate in Parkinson's disease.
 *
 * <p>Source: Dalrymple-Alford JC, MacAskill MR, Nakas CT, et al. <em>The MoCA: well-suited
 * screen for cognitive impairment in Parkinson disease.</em> Neurology. 2010;75(19):1717–1725.
 * A total below 26 discriminated mild cognitive impairment (sensitivity 90%, specificity 75%);
 * a total below 21 discriminated dementia. The study reported the two analyses separately, so
 * the sensitivity figures are not transferable between bands.
 *
 * <p>These are <strong>screening</strong> thresholds. Nothing here diagnoses anyone, and the
 * wording that reaches the patient must not suggest otherwise.
 */
public final class CognitionRule {

    /** Below this, the study discriminated mild cognitive impairment in Parkinson's disease. */
    static final int MCI_CUT_OFF = 26;

    /** Below this, the study discriminated Parkinson's disease dementia. */
    static final int DEMENTIA_CUT_OFF = 21;

    private static final String SOURCE =
            "Dalrymple-Alford et al., Neurology 2010 (Parkinson's-specific MoCA cut-offs)";

    private CognitionRule() {
    }

    /** Empty when the record holds no MoCA score; there is then nothing to say about it. */
    public static Optional<ClinicalFinding> evaluate(PatientSnapshot snapshot) {
        List<ScoredAssessment> series = snapshot.moca();
        if (series.isEmpty()) {
            return Optional.empty();
        }

        ScoredAssessment latest = series.get(series.size() - 1);
        return Optional.of(new ClinicalFinding(
                "Cognition (MoCA)",
                String.format(Locale.ENGLISH, "%,.0f out of 30, recorded %s",
                        latest.score(), latest.date()),
                band(latest.score()),
                Trends.describe(series, "points", Trends.Direction.LOWER_IS_WORSE),
                SOURCE));
    }

    private static String band(double score) {
        if (score < DEMENTIA_CUT_OFF) {
            return "below the screening threshold of " + DEMENTIA_CUT_OFF
                    + " used to identify dementia in Parkinson's disease";
        }
        if (score < MCI_CUT_OFF) {
            return "below the screening threshold of " + MCI_CUT_OFF
                    + " used to identify mild cognitive impairment in Parkinson's disease, "
                    + "but above the dementia threshold of " + DEMENTIA_CUT_OFF;
        }
        return "at or above the screening threshold of " + MCI_CUT_OFF
                + ", so this screen did not flag cognitive impairment";
    }
}
