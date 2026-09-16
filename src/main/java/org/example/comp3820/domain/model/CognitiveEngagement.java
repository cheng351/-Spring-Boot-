package org.example.comp3820.domain.model;

/**
 * Progress against the cognitive and social activities in the patient's FHIR CarePlan.
 *
 * <p>There is no LOINC code for "cognitive engagement" as an activity count — searching the
 * terminology server for {@code cognitive} returns cognitive <em>assessments</em> (MoCA,
 * AM-PAC) rather than participation. We therefore read engagement from CarePlan activities
 * rather than inventing an Observation code for it. See the FHIR resource justification in
 * the team report.
 *
 * @param completedThisWeek activities the patient has completed so far this week
 * @param weeklyGoal        activities the care plan asks for each week
 */
public record CognitiveEngagement(int completedThisWeek, int weeklyGoal) {

    public CognitiveEngagement {
        if (completedThisWeek < 0 || weeklyGoal < 0) {
            throw new IllegalArgumentException("activity counts cannot be negative");
        }
    }

    /** How many more activities would meet the goal; zero once the goal is met. */
    public int remaining() {
        return Math.max(0, weeklyGoal - completedThisWeek);
    }
}
