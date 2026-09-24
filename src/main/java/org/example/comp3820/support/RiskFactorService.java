package org.example.comp3820.support;

import org.example.comp3820.domain.model.ClinicalFinding;
import org.example.comp3820.domain.model.PatientIdentity;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.RiskProfile;
import org.example.comp3820.domain.rules.CognitionRule;
import org.example.comp3820.domain.rules.MoodRule;
import org.example.comp3820.domain.rules.MotorRule;
import org.example.comp3820.fhir.ClinicalDataGateway;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Composes the FHIR gateway with the clinical rules to produce a patient's risk profile.
 *
 * <p>Deliberately thin: it fetches once and applies each rule to the same snapshot, so the
 * findings are always consistent with one another and with a single read of the record.
 * Anything resembling a clinical judgement belongs in {@code domain.rules}, where the
 * thresholds sit next to their citations and can be tested; anything resembling a FHIR query
 * belongs behind {@link ClinicalDataGateway}.
 *
 * <p>Nothing here calls the language model. The profile is what the model is later given as
 * established fact — the numbers and the thresholds are settled before any prompt is built.
 */
@Service
public class RiskFactorService {

    private final ClinicalDataGateway gateway;

    public RiskFactorService(ClinicalDataGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * Everything the rules can establish from this patient's record.
     *
     * <p>Instruments the record does not carry produce no finding at all, rather than a finding
     * that says "unknown": an absent heading is simply not a fact, whereas an explicit "no data"
     * line invites the assistant to speculate about why.
     */
    public RiskProfile profileFor(String patientId) {
        PatientSnapshot snapshot = gateway.fetchSnapshot(patientId);

        List<ClinicalFinding> findings = new ArrayList<>();
        addIfPresent(findings, CognitionRule.evaluate(snapshot));
        addIfPresent(findings, MoodRule.evaluate(snapshot));
        addIfPresent(findings, MotorRule.evaluate(snapshot));

        return new RiskProfile(findings, snapshot.conditions());
    }

    /** Identifying details for the greeting and the record drawer. Never sent to the model. */
    public PatientIdentity identityFor(String patientId) {
        return gateway.fetchIdentity(patientId);
    }

    private static void addIfPresent(List<ClinicalFinding> findings,
                                     Optional<ClinicalFinding> finding) {
        finding.ifPresent(findings::add);
    }
}
