package org.example.comp3820.fhir;

import org.example.comp3820.domain.model.PatientIdentity;
import org.example.comp3820.domain.model.PatientSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link ClinicalDataGateway} backed by a map instead of a FHIR server.
 *
 * <p>This is the test double that makes the layers above the gateway testable. Because it
 * implements the same interface the production code depends on, a test can exercise the whole
 * path from a patient id to a set of dashboard cards with no network, no sandbox account and no
 * fixture data to keep in sync with someone else's server.
 *
 * <p>It also raises the same two exceptions the real gateway does, so error handling can be
 * tested rather than assumed.
 */
public class InMemoryClinicalDataGateway implements ClinicalDataGateway {

    private final Map<String, PatientSnapshot> snapshots = new HashMap<>();
    private final Map<String, PatientIdentity> identities = new HashMap<>();
    private boolean serverDown;

    public InMemoryClinicalDataGateway withPatient(String patientId, PatientSnapshot snapshot) {
        snapshots.put(patientId, snapshot);
        return this;
    }

    public InMemoryClinicalDataGateway withIdentity(String patientId, PatientIdentity identity) {
        identities.put(patientId, identity);
        return this;
    }

    /** Makes every subsequent call fail the way an unreachable FHIR server would. */
    public InMemoryClinicalDataGateway withServerDown() {
        this.serverDown = true;
        return this;
    }

    @Override
    public PatientIdentity fetchIdentity(String patientId) {
        failIfServerDown();
        PatientIdentity identity = identities.get(patientId);
        if (identity == null) {
            throw new PatientNotFoundException(patientId, null);
        }
        return identity;
    }

    @Override
    public PatientSnapshot fetchSnapshot(String patientId) {
        failIfServerDown();
        PatientSnapshot snapshot = snapshots.get(patientId);
        if (snapshot == null) {
            throw new PatientNotFoundException(patientId, null);
        }
        return snapshot;
    }

    private void failIfServerDown() {
        if (serverDown) {
            throw new ClinicalDataUnavailableException("simulated outage", null);
        }
    }
}
