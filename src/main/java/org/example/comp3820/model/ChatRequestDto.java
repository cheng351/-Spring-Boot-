package org.example.comp3820.model;

import java.util.List;

/**
 * A question, together with what has already been said.
 *
 * <p>The conversation is held by the caller rather than on the server. That keeps this service
 * stateless, and it means no health information is retained between requests — which is what the
 * front end tells the patient happens.
 *
 * @param patientId the FHIR Patient logical id
 * @param question  what the patient has just asked
 * @param history   earlier turns, oldest first; may be null or empty for the first question
 */
public record ChatRequestDto(
        String patientId,
        String question,
        List<ChatTurnDto> history
) {
    public ChatRequestDto {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
