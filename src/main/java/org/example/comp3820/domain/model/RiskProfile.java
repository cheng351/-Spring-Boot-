package org.example.comp3820.domain.model;

import java.util.List;

/**
 * Everything the rules were able to establish about one patient.
 *
 * <p>Replaces the earlier card-shaped model. The front end no longer shows risk factor cards,
 * so the useful output is no longer three tiles but a set of statements that can be handed to
 * the assistant as given facts.
 *
 * @param findings   one entry per instrument the record actually carries, in reporting order
 * @param conditions display text of the patient's coded diagnoses
 */
public record RiskProfile(List<ClinicalFinding> findings, List<String> conditions) {

    public RiskProfile {
        findings = List.copyOf(findings);
        conditions = List.copyOf(conditions);
    }

    public boolean isEmpty() {
        return findings.isEmpty();
    }
}
