package org.example.comp3820.domain.model;

import java.util.List;

/**
 * Everything the risk rules and the AI agent need to reason about one patient — and
 * deliberately nothing more.
 *
 * <p><strong>This type is the privacy boundary.</strong> It carries no name, no identifier and
 * no date of birth, because instances of it are what get turned into prompts and sent to a
 * third-party LLM. Anything the patient would be identified by is kept in the profile path
 * (which is rendered by the front end and never leaves our own service). Adding an identifying
 * field here would silently widen what we disclose to Google, so don't.
 *
 * <p>Age is the one borderline field and it is kept because the thresholds need it: the GDS-15
 * screening cut-off shifts above 75.
 *
 * <p>Each assessment series is ordered oldest first, so a trend reads left to right.
 *
 * @param age        age in whole years
 * @param gender     administrative gender as recorded on the FHIR Patient
 * @param conditions display text of the patient's coded conditions, e.g. "Parkinson's disease"
 * @param moca       Montreal Cognitive Assessment totals, 0–30, lower is worse
 * @param updrs      UPDRS totals as recorded; no severity is derived from these, see the rule
 * @param gds15      Geriatric Depression Scale (short form) totals, 0–15, higher is worse
 */
public record PatientSnapshot(
        int age,
        String gender,
        List<String> conditions,
        List<ScoredAssessment> moca,
        List<ScoredAssessment> updrs,
        List<ScoredAssessment> gds15
) {

    public PatientSnapshot {
        conditions = List.copyOf(conditions);
        moca = List.copyOf(moca);
        updrs = List.copyOf(updrs);
        gds15 = List.copyOf(gds15);
    }
}
