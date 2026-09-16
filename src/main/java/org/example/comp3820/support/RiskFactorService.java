package org.example.comp3820.support;

import org.example.comp3820.domain.model.PatientIdentity;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.RiskFactor;
import org.example.comp3820.domain.rules.ActivityRiskRule;
import org.example.comp3820.domain.rules.CognitiveEngagementRule;
import org.example.comp3820.domain.rules.SleepRiskRule;
import org.example.comp3820.fhir.ClinicalDataGateway;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Composes the FHIR gateway with the risk rules to produce the dashboard cards.
 *
 * <p>Deliberately thin: it fetches once and applies each rule to the same snapshot, so the three
 * cards are always consistent with one another and with a single read of the record. Anything
 * resembling a clinical decision belongs in {@code domain.rules}, where it can be tested;
 * anything resembling a FHIR query belongs behind {@link ClinicalDataGateway}.
 *
 * <p>Nothing here calls the language model. The prose half of each card is added later, by the
 * AI layer, using these values as given facts.
 */
@Service
public class RiskFactorService {

    private final ClinicalDataGateway gateway;
    private final Clock clock;

    public RiskFactorService(ClinicalDataGateway gateway, Clock clock) {
        this.gateway = gateway;
        this.clock = clock;
    }

    /**
     * The three modifiable dementia risk factors named in the project brief, in the order the
     * dashboard shows them.
     */
    public List<RiskFactor> factorsFor(String patientId) {
        PatientSnapshot snapshot = gateway.fetchSnapshot(patientId);
        LocalDate today = LocalDate.now(clock);

        return List.of(
                SleepRiskRule.evaluate(snapshot, today),
                ActivityRiskRule.evaluate(snapshot, today),
                CognitiveEngagementRule.evaluate(snapshot));
    }

    /** Identifying details for the greeting and the record drawer. Never sent to the model. */
    public PatientIdentity identityFor(String patientId) {
        return gateway.fetchIdentity(patientId);
    }
}
