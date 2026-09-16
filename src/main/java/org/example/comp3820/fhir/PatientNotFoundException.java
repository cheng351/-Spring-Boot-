package org.example.comp3820.fhir;

/**
 * Raised when the FHIR server has no patient with the requested id.
 *
 * <p>Distinct from {@link ClinicalDataUnavailableException} so that the web layer can answer
 * 404 rather than 500 — asking for a patient who does not exist is a client mistake, not a
 * server fault.
 */
public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(String patientId, Throwable cause) {
        super("No patient with id " + patientId, cause);
    }
}
