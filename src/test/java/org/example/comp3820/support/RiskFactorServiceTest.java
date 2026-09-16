package org.example.comp3820.support;

import org.example.comp3820.domain.model.CognitiveEngagement;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.RiskFactor;
import org.example.comp3820.domain.model.RiskLevel;
import org.example.comp3820.domain.model.SleepRecord;
import org.example.comp3820.domain.model.StepRecord;
import org.example.comp3820.fhir.ClinicalDataUnavailableException;
import org.example.comp3820.fhir.InMemoryClinicalDataGateway;
import org.example.comp3820.fhir.PatientNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the whole path from a patient id to a set of dashboard cards, with no FHIR server
 * and no language model in the loop.
 */
class RiskFactorServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Australia/Brisbane");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 16);
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-16T03:00:00Z"), ZONE);

    private static PatientSnapshot snapshot(double sleepHours, int steps, int done, int goal) {
        List<SleepRecord> nights = new ArrayList<>();
        List<StepRecord> days = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            nights.add(new SleepRecord(TODAY.minusDays(i), sleepHours));
            days.add(new StepRecord(TODAY.minusDays(i), steps));
        }
        return new PatientSnapshot(68, "female", List.of("Parkinson's disease"),
                nights, days, new CognitiveEngagement(done, goal));
    }

    private static RiskFactorService serviceFor(PatientSnapshot snapshot) {
        return new RiskFactorService(
                new InMemoryClinicalDataGateway().withPatient("p1", snapshot), FIXED_CLOCK);
    }

    @Test
    void producesTheThreeCardsInDashboardOrder() {
        List<RiskFactor> factors = serviceFor(snapshot(8.0, 6000, 3, 3)).factorsFor("p1");

        assertThat(factors).extracting(RiskFactor::title)
                .containsExactly("Sleep", "Movement", "Mind & connection");
    }

    @Test
    void eachCardNamesTheFhirResourceItsFigureCameFrom() {
        List<RiskFactor> factors = serviceFor(snapshot(8.0, 6000, 3, 3)).factorsFor("p1");

        assertThat(factors).extracting(RiskFactor::fhirResource)
                .containsExactly("Observation", "Observation", "CarePlan");
    }

    @Test
    void aHealthyPatientGetsThreeReassuringCards() {
        List<RiskFactor> factors = serviceFor(snapshot(8.0, 6000, 3, 3)).factorsFor("p1");

        assertThat(factors).allSatisfy(
                factor -> assertThat(factor.level()).isEqualTo(RiskLevel.ON_TRACK));
    }

    /**
     * The brief asks that the tool "responds appropriately across a range of risk profiles", so
     * a patient at the other end of the range must produce visibly different cards.
     */
    @Test
    void aPatientAtRiskGetsCardsThatSaySo() {
        List<RiskFactor> factors = serviceFor(snapshot(5.0, 800, 0, 3)).factorsFor("p1");

        assertThat(factors).allSatisfy(
                factor -> assertThat(factor.level()).isEqualTo(RiskLevel.WORTH_ATTENTION));
        assertThat(factors).extracting(factor -> factor.level().tone())
                .containsExactly("amber", "amber", "amber");
    }

    @Test
    void riskFactorsAreGradedIndependentlyOfOneAnother() {
        // sleeping badly but moving well, and part-way through the care plan
        List<RiskFactor> factors = serviceFor(snapshot(5.0, 6000, 2, 3)).factorsFor("p1");

        assertThat(factors).extracting(RiskFactor::level).containsExactly(
                RiskLevel.WORTH_ATTENTION, RiskLevel.ON_TRACK, RiskLevel.IN_PROGRESS);
    }

    @Test
    void figuresOnTheCardsComeFromTheRecordRatherThanBeingGenerated() {
        List<RiskFactor> factors = serviceFor(snapshot(6.5, 4280, 2, 3)).factorsFor("p1");

        assertThat(factors.get(0).detail()).isEqualTo("6 h 30 min last night");
        assertThat(factors.get(1).detail()).isEqualTo("4,280 steps yesterday");
        assertThat(factors.get(2).detail()).isEqualTo("2 activities this week");
    }

    @Test
    void anUnknownPatientIsReportedAsNotFoundRatherThanAsAServerFault() {
        RiskFactorService service = serviceFor(snapshot(8.0, 6000, 3, 3));

        assertThatThrownBy(() -> service.factorsFor("nobody"))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void anUnreachableServerSurfacesAsAnAvailabilityFailure() {
        RiskFactorService service = new RiskFactorService(
                new InMemoryClinicalDataGateway().withServerDown(), FIXED_CLOCK);

        assertThatThrownBy(() -> service.factorsFor("p1"))
                .isInstanceOf(ClinicalDataUnavailableException.class);
    }
}
