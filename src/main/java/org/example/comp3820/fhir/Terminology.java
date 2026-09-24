package org.example.comp3820.fhir;

/**
 * Coded concepts this application reads from the FHIR server.
 *
 * <p>Every code below was checked against the public terminology server at
 * {@code https://tx.fhir.org/r4} using {@code CodeSystem/$lookup}; the comment beside each one
 * is the display name that server returned, not a remembered value. Codes are easy to get
 * subtly wrong from memory — {@code 89006-1}, for instance, looks like a sleep questionnaire
 * but is actually a p53 antibody assay — so verify any code added here before relying on it.
 *
 * <p>The choice of these particular codes is part of the FHIR resource justification in the
 * team report.
 */
public final class Terminology {

    public static final String LOINC = "http://loinc.org";
    public static final String SNOMED = "http://snomed.info/sct";

    // --- diagnoses -------------------------------------------------------------------------

    /** SNOMED CT: "Parkinson's disease" — the cohort's defining diagnosis. */
    public static final String PARKINSONS_DISEASE = "49049000";

    /** SNOMED CT: "Dementia associated with Parkinson's disease". */
    public static final String PARKINSONS_DEMENTIA = "191449007";

    /** SNOMED CT: "Mild cognitive disorder". */
    public static final String MILD_COGNITIVE_DISORDER = "386806002";

    /** SNOMED CT: "Rapid eye movement sleep behavior disorder" — a recognised prodromal marker. */
    public static final String REM_SLEEP_BEHAVIOUR_DISORDER = "709186000";

    /** SNOMED CT: "Depressive disorder". */
    public static final String DEPRESSIVE_DISORDER = "35489007";

    /** SNOMED CT: "Hallucinations". */
    public static final String HALLUCINATIONS = "7011001";

    /** SNOMED CT: "Orthostatic hypotension". */
    public static final String ORTHOSTATIC_HYPOTENSION = "28651003";

    /** SNOMED CT: "Loss of sense of smell (hyposmia)" — a recognised prodromal marker. */
    public static final String HYPOSMIA = "44169009";

    // --- scored assessments ----------------------------------------------------------------

    /** LOINC: "Total score [MoCA]" — 0–30, lower is worse. */
    public static final String MOCA_TOTAL = "72172-0";

    /**
     * LOINC: "Geriatric depression scale (GDS) short version total" — 0–15, higher is worse.
     *
     * <p>Not to be confused with {@code 48544-1}, which is the 30-item version's total.
     */
    public static final String GDS15_TOTAL = "48545-8";

    /**
     * LOINC: "Unified Parkinson's Disease Rating Scale (UPDRS) panel".
     *
     * <p><strong>A deliberate compromise.</strong> LOINC models the UPDRS as a panel of
     * individual items and offers no code for a total score, so this panel code is carried on
     * an Observation holding a scalar total, with the original wording kept in
     * {@code CodeableConcept.text}. A consumer expecting panel structure will find no
     * {@code hasMember} entries.
     *
     * <p>This is also why no severity banding is derived from it: the published cut-offs
     * (Martínez-Martín et al. 2015) are defined per MDS-UPDRS part, not for a total, and the
     * dataset records an unsplit total of an unspecified UPDRS version.
     */
    public static final String UPDRS_PANEL = "77717-7";

    private Terminology() {
    }
}
