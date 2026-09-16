package org.example.comp3820.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.example.comp3820.domain.model.CognitiveEngagement;
import org.example.comp3820.domain.model.PatientIdentity;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.SleepRecord;
import org.example.comp3820.domain.model.StepRecord;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads patient data from a FHIR R4 server using the HAPI client.
 *
 * <p>This is the only class in the application that knows about HAPI types. Everything above it
 * works with the plain records in {@code domain.model}, which is what lets the risk rules be
 * tested without a server.
 */
@Component
public class HapiClinicalDataGateway implements ClinicalDataGateway {

    private static final Logger log = LoggerFactory.getLogger(HapiClinicalDataGateway.class);

    /** Resources requested per page. */
    private static final int PAGE_SIZE = 50;

    /**
     * Safety valve on pagination. A synthetic patient should never need this many pages; the cap
     * stops a misconfigured server or a runaway cohort from hanging a patient-facing request.
     */
    private static final int MAX_PAGES = 10;

    private final IGenericClient client;

    public HapiClinicalDataGateway(IGenericClient client) {
        this.client = client;
    }

    @Override
    public PatientIdentity fetchIdentity(String patientId) {
        Patient patient = readPatient(patientId);
        return new PatientIdentity(
                patientId,
                displayName(patient),
                ageOf(patient, LocalDate.now()),
                patient.hasGender() ? patient.getGender().getDisplay() : "Unknown",
                conditionsOf(patientId));
    }

    @Override
    public PatientSnapshot fetchSnapshot(String patientId) {
        Patient patient = readPatient(patientId);
        return new PatientSnapshot(
                ageOf(patient, LocalDate.now()),
                patient.hasGender() ? patient.getGender().getDisplay() : "Unknown",
                conditionsOf(patientId),
                sleepOf(patientId),
                stepsOf(patientId),
                cognitiveEngagementOf(patientId));
    }

    // --- resource reads -------------------------------------------------------------------

    private Patient readPatient(String patientId) {
        try {
            return client.read().resource(Patient.class).withId(patientId).execute();
        } catch (ResourceNotFoundException e) {
            throw new PatientNotFoundException(patientId, e);
        } catch (RuntimeException e) {
            throw new ClinicalDataUnavailableException("Could not read Patient/" + patientId, e);
        }
    }

    private List<String> conditionsOf(String patientId) {
        Bundle bundle = search(Condition.class, patientId, null);
        return resources(bundle, Condition.class).stream()
                .map(this::conditionText)
                .flatMap(Optional::stream)
                .distinct()
                .toList();
    }

    /**
     * Prefers the code's free text, falling back to the first coding's display, so that a
     * resource coded without free text is not silently dropped.
     */
    private Optional<String> conditionText(Condition condition) {
        if (!condition.hasCode()) {
            return Optional.empty();
        }
        if (condition.getCode().hasText()) {
            return Optional.of(condition.getCode().getText());
        }
        return condition.getCode().getCoding().stream()
                .filter(coding -> coding.hasDisplay())
                .map(coding -> coding.getDisplay())
                .findFirst();
    }

    private List<SleepRecord> sleepOf(String patientId) {
        Bundle bundle = search(Observation.class, patientId, Terminology.SLEEP_DURATION);
        List<SleepRecord> records = new ArrayList<>();
        for (Observation observation : resources(bundle, Observation.class)) {
            Optional<LocalDate> date = effectiveDate(observation);
            Optional<Double> hours = sleepHours(observation);
            if (date.isPresent() && hours.isPresent()) {
                records.add(new SleepRecord(date.get(), hours.get()));
            }
        }
        return records;
    }

    private List<StepRecord> stepsOf(String patientId) {
        Bundle bundle = search(Observation.class, patientId, Terminology.STEPS_24H);
        List<StepRecord> records = new ArrayList<>();
        for (Observation observation : resources(bundle, Observation.class)) {
            Optional<LocalDate> date = effectiveDate(observation);
            if (date.isPresent() && observation.hasValueQuantity()
                    && observation.getValueQuantity().hasValue()) {
                records.add(new StepRecord(date.get(),
                        observation.getValueQuantity().getValue().intValue()));
            }
        }
        return records;
    }

    /**
     * Derives cognitive-engagement progress from the patient's active care plans.
     *
     * <p><strong>Assumption worth stating in the report:</strong> each activity on an active
     * CarePlan counts as one item towards the weekly goal, and an activity counts as done when
     * its detail status is {@code completed}. This is a modelling choice, not something FHIR
     * dictates, since CarePlan has no notion of "per week". It needs confirming with the mentor.
     */
    private CognitiveEngagement cognitiveEngagementOf(String patientId) {
        Bundle bundle = search(CarePlan.class, patientId, null);
        int goal = 0;
        int done = 0;
        for (CarePlan plan : resources(bundle, CarePlan.class)) {
            if (plan.getStatus() != CarePlan.CarePlanStatus.ACTIVE) {
                continue;
            }
            for (CarePlan.CarePlanActivityComponent activity : plan.getActivity()) {
                goal++;
                if (activity.hasDetail() && activity.getDetail().hasStatus()
                        && activity.getDetail().getStatus()
                                == CarePlan.CarePlanActivityStatus.COMPLETED) {
                    done++;
                }
            }
        }
        return new CognitiveEngagement(done, goal);
    }

    // --- search plumbing ------------------------------------------------------------------

    private Bundle search(Class<? extends Resource> type, String patientId, String loincCode) {
        try {
            IQuery<Bundle> query = client.search().forResource(type)
                    .where(new TokenClientParam("patient").exactly().code(patientId))
                    .count(PAGE_SIZE)
                    .returnBundle(Bundle.class);
            if (loincCode != null) {
                query = query.and(new TokenClientParam("code")
                        .exactly().systemAndCode(Terminology.LOINC, loincCode));
            }
            return query.execute();
        } catch (RuntimeException e) {
            throw new ClinicalDataUnavailableException(
                    "Could not search " + type.getSimpleName() + " for patient " + patientId, e);
        }
    }

    /**
     * Collects every resource of the given type across all pages of a search result.
     *
     * <p>Following the {@code next} link matters: a patient with a week of daily observations
     * already exceeds a small default page size on some servers, and reading only the first
     * page would silently drop the older half of the window the risk rules average over.
     */
    private <T extends Resource> List<T> resources(Bundle bundle, Class<T> type) {
        List<T> collected = new ArrayList<>();
        int pages = 0;
        Bundle page = bundle;
        while (page != null) {
            for (Bundle.BundleEntryComponent entry : page.getEntry()) {
                Resource resource = entry.getResource();
                if (type.isInstance(resource)) {
                    collected.add(type.cast(resource));
                }
            }
            pages++;
            if (page.getLink(Bundle.LINK_NEXT) == null) {
                break;
            }
            if (pages >= MAX_PAGES) {
                log.warn("Stopped paging {} after {} pages for safety; results may be truncated",
                        type.getSimpleName(), pages);
                break;
            }
            try {
                page = client.loadPage().next(page).execute();
            } catch (RuntimeException e) {
                throw new ClinicalDataUnavailableException(
                        "Could not load page " + (pages + 1) + " of " + type.getSimpleName(), e);
            }
        }
        return collected;
    }

    // --- value extraction -----------------------------------------------------------------

    private String displayName(Patient patient) {
        if (patient.hasName() && !patient.getName().isEmpty()) {
            return patient.getNameFirstRep().getNameAsSingleString();
        }
        return "Unknown";
    }

    /**
     * Age in whole years, or {@code 0} when the birth date is absent or unparseable.
     *
     * <p>FHIR dates may be partial ({@code 1958}, {@code 1958-03}), so the string is padded
     * rather than handed straight to {@link LocalDate#parse}. Working from the string also
     * avoids the off-by-one that {@code java.util.Date} conversion introduces when the server's
     * timezone differs from ours.
     */
    private int ageOf(Patient patient, LocalDate today) {
        if (!patient.hasBirthDateElement() || patient.getBirthDateElement().isEmpty()) {
            return 0;
        }
        return parseFhirDate(patient.getBirthDateElement().getValueAsString())
                .map(birth -> Period.between(birth, today).getYears())
                .orElse(0);
    }

    private Optional<LocalDate> effectiveDate(Observation observation) {
        if (observation.hasEffectiveDateTimeType()) {
            return parseFhirDate(observation.getEffectiveDateTimeType().getValueAsString());
        }
        if (observation.hasEffectiveInstantType()) {
            return parseFhirDate(observation.getEffectiveInstantType().getValueAsString());
        }
        if (observation.hasEffectivePeriod() && observation.getEffectivePeriod().hasStart()) {
            return parseFhirDate(
                    observation.getEffectivePeriod().getStartElement().getValueAsString());
        }
        return Optional.empty();
    }

    /** Parses a FHIR date, dateTime or instant down to its calendar date. */
    private Optional<LocalDate> parseFhirDate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String text;
        if (value.length() >= 10) {
            text = value.substring(0, 10);
        } else if (value.length() == 7) {
            text = value + "-01";
        } else if (value.length() == 4) {
            text = value + "-01-01";
        } else {
            text = value;
        }
        try {
            return Optional.of(LocalDate.parse(text));
        } catch (DateTimeParseException e) {
            log.warn("Ignoring unparseable FHIR date '{}'", value);
            return Optional.empty();
        }
    }

    /**
     * Sleep duration in hours.
     *
     * <p>The unit is honoured rather than assumed: the same LOINC code is recorded in hours by
     * some sources and in minutes by others, and treating 420 min as 420 h would put the card
     * into a nonsensical state.
     */
    private Optional<Double> sleepHours(Observation observation) {
        if (!observation.hasValueQuantity() || !observation.getValueQuantity().hasValue()) {
            return Optional.empty();
        }
        Quantity quantity = observation.getValueQuantity();
        double value = quantity.getValue().doubleValue();
        String unit;
        if (quantity.hasCode()) {
            unit = quantity.getCode();
        } else if (quantity.hasUnit()) {
            unit = quantity.getUnit();
        } else {
            unit = "h";
        }

        switch (unit.toLowerCase()) {
            case "min":
            case "minute":
            case "minutes":
                return Optional.of(value / 60.0);
            case "s":
            case "sec":
            case "second":
            case "seconds":
                return Optional.of(value / 3600.0);
            case "h":
            case "hr":
            case "hour":
            case "hours":
                return Optional.of(value);
            default:
                log.warn("Unrecognised sleep duration unit '{}', assuming hours", unit);
                return Optional.of(value);
        }
    }
}
