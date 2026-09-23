package org.example.comp3820.model;

/**
 * One turn of the conversation, as the front end holds it.
 *
 * @param role either {@code user} or {@code assistant}; anything else is ignored
 * @param text what was said
 */
public record ChatTurnDto(String role, String text) {
}
