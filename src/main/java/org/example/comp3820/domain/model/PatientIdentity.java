package org.example.comp3820.domain.model;

import java.util.List;

/**
 * The identifying details the front end needs to greet the patient and show their record.
 *
 * <p>Kept deliberately separate from {@link PatientSnapshot}: this type is rendered by our own
 * UI and never reaches the language model, whereas the snapshot is turned into prompts. If you
 * find yourself wanting a field from here inside a prompt, that is the signal to stop and think
 * about what is being disclosed, not to merge the two records.
 *
 * @param patientId  the FHIR Patient logical id
 * @param name       the patient's display name
 * @param age        age in whole years
 * @param gender     administrative gender
 * @param conditions display text of the patient's coded conditions
 */
public record PatientIdentity(
        String patientId,
        String name,
        int age,
        String gender,
        List<String> conditions
) {
    public PatientIdentity {
        conditions = List.copyOf(conditions);
    }
}
