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

    /** SNOMED CT: "Parkinson's disease" — the cohort's defining diagnosis. */
    public static final String PARKINSONS_DISEASE = "49049000";

    /** LOINC: "Sleep duration". */
    public static final String SLEEP_DURATION = "93832-4";

    /** LOINC: "Number of steps in 24 hour Measured". */
    public static final String STEPS_24H = "41950-7";

    /** LOINC: "Montreal Cognitive Assessment [MoCA]" — cognitive status, not engagement. */
    public static final String MOCA = "72133-2";

    private Terminology() {
    }
}
