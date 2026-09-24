package org.example.comp3820.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.example.comp3820.domain.model.PatientIdentity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Exercises the real gateway against the team's local HAPI FHIR server.
 *
 * <p>Skips itself when the server is not running, so it is safe to leave in the suite: a
 * developer without Docker up still gets a green build. Run the team's package first
 * ({@code ./start.sh} in {@code hapi_fhir_backend_package}) to have it actually execute.
 *
 * <p>Only {@link ClinicalDataGateway#fetchIdentity} is asserted here. The snapshot path reads
 * sleep, step and care-plan data that this dataset does not carry, and is being redirected at
 * the codes the dataset actually uses.
 */
class HapiClinicalDataGatewayLiveTest {

    private static final String BASE_URL = "http://localhost:8080/fhir";

    /** patient_001 in the team dataset: Jennifer Johnson, born 1966-11-24. */
    private static final String PATIENT_ID = "25b3eb56-3d12-4496-a4ba-f5fd0913b3c9";

    private static boolean serverUp;

    @BeforeAll
    static void probeServer() {
        try {
            HttpResponse<Void> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build()
                    .send(HttpRequest.newBuilder(URI.create(BASE_URL + "/metadata"))
                                    .timeout(Duration.ofSeconds(5))
                                    .build(),
                            HttpResponse.BodyHandlers.discarding());
            serverUp = response.statusCode() == 200;
        } catch (Exception e) {
            serverUp = false;
        }
    }

    private ClinicalDataGateway gateway() {
        IGenericClient client = FhirContext.forR4().newRestfulGenericClient(BASE_URL);
        return new HapiClinicalDataGateway(client);
    }

    @Test
    void readsIdentityFromTheLiveServer() {
        assumeTrue(serverUp, "local HAPI FHIR server is not running");

        PatientIdentity identity = gateway().fetchIdentity(PATIENT_ID);

        assertThat(identity.patientId()).isEqualTo(PATIENT_ID);
        assertThat(identity.name()).isEqualTo("Jennifer Johnson");
        assertThat(identity.gender()).isEqualToIgnoringCase("Female");
        assertThat(identity.age()).isEqualTo(59);
    }

    @Test
    void readsEveryConditionForThePatient() {
        assumeTrue(serverUp, "local HAPI FHIR server is not running");

        PatientIdentity identity = gateway().fetchIdentity(PATIENT_ID);

        assertThat(identity.conditions())
                .contains("Parkinson's disease")
                .hasSize(6);
    }

    @Test
    void reportsAMissingPatientAsNotFound() {
        assumeTrue(serverUp, "local HAPI FHIR server is not running");

        org.junit.jupiter.api.Assertions.assertThrows(
                PatientNotFoundException.class,
                () -> gateway().fetchIdentity("no-such-patient-comp3820"));
    }
}
