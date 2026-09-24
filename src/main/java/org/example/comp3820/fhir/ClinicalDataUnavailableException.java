package org.example.comp3820.fhir;

/**
 * Raised when the FHIR server could not be reached, timed out, or returned an error.
 *
 * <p>Carries a technical message for the logs. The patient-facing wording is chosen by the web
 * layer, which should never expose this message directly.
 */
public class ClinicalDataUnavailableException extends RuntimeException {

    public ClinicalDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
