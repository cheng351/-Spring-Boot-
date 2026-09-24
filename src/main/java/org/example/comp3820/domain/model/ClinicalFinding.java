package org.example.comp3820.domain.model;

/**
 * One statement about the patient's record that this application is prepared to stand behind.
 *
 * <p>Each field is computed from FHIR data by a rule in {@code domain.rules}, never by the
 * language model. The model is given these as settled facts and asked only to put them into
 * plain words — so a wrong number here is a bug we can find and fix, rather than a fabrication
 * we cannot see.
 *
 * @param label          what this finding is about, e.g. "Cognition (MoCA)"
 * @param measurement    the most recent value and when it was taken
 * @param interpretation what the value means against a published threshold, or null when no
 *                       validated threshold exists for this measure
 * @param trend          how it has moved across earlier recordings, or null when there is only
 *                       one recording
 * @param basis          the source of the threshold used, or null when none was applied
 */
public record ClinicalFinding(
        String label,
        String measurement,
        String interpretation,
        String trend,
        String basis
) {
}
