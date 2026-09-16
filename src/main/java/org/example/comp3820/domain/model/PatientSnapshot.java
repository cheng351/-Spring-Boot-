package org.example.comp3820.domain.model;

import java.util.List;

/**
 * Everything the risk rules and the AI agent need to reason about one patient — and
 * deliberately nothing more.
 *
 * <p><strong>This type is the privacy boundary.</strong> It carries no name, no identifier and
 * no date of birth, because instances of it are what get turned into prompts and sent to a
 * third-party LLM. Anything the patient would be identified by is kept in the profile path
 * (which is rendered by the front end and never leaves our own service). Adding an identifying
 * field here would silently widen what we disclose to Google, so don't.
 *
 * @param age         age in whole years
 * @param gender      administrative gender as recorded on the FHIR Patient
 * @param conditions  display text of the patient's coded conditions, e.g. "Parkinson's disease"
 * @param sleep       recent nights of sleep, most recent first
 * @param steps       recent days of step counts, most recent first
 * @param cognitive   progress against the care plan's cognitive activities
 */
public record PatientSnapshot(
        int age,
        String gender,
        List<String> conditions,
        List<SleepRecord> sleep,
        List<StepRecord> steps,
        CognitiveEngagement cognitive
) {

    public PatientSnapshot {
        conditions = List.copyOf(conditions);
        sleep = List.copyOf(sleep);
        steps = List.copyOf(steps);
    }
}
