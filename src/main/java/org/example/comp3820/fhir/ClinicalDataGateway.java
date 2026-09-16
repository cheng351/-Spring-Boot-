package org.example.comp3820.fhir;

import org.example.comp3820.domain.model.PatientIdentity;
import org.example.comp3820.domain.model.PatientSnapshot;

/**
 * Reads patient data from a FHIR server.
 *
 * <p>This interface is the seam that keeps HAPI FHIR out of the domain layer. Production code
 * binds it to {@link HapiClinicalDataGateway}; tests bind it to an in-memory fake, which is
 * what allows the risk rules and the orchestration above them to be exercised without a live
 * FHIR server.
 *
 * <p>The two methods return deliberately different views of the same patient — see
 * {@link PatientIdentity} and {@link PatientSnapshot} for why they are not one type.
 */
public interface ClinicalDataGateway {

    /**
     * Identifying details for display.
     *
     * @throws PatientNotFoundException      if no such patient exists on the server
     * @throws ClinicalDataUnavailableException if the server could not be reached or replied with an error
     */
    PatientIdentity fetchIdentity(String patientId);

    /**
     * De-identified clinical data for the risk rules and the AI agent.
     *
     * @throws PatientNotFoundException      if no such patient exists on the server
     * @throws ClinicalDataUnavailableException if the server could not be reached or replied with an error
     */
    PatientSnapshot fetchSnapshot(String patientId);
}
