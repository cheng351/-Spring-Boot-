package org.example.comp3820.domain.model;

/**
 * The status a risk factor can be in, as presented to the patient.
 *
 * <p>Each level carries both the wording and the colour tone the UI uses, so that the
 * front end never has to re-derive one from the other. The three values match the
 * {@code tone} union already used by the dashboard cards ({@code 'amber' | 'green' | 'blue'}).
 *
 * <p>Note that these are deliberately not severity ranks: {@link #IN_PROGRESS} describes a
 * patient who is part-way towards a care-plan goal, which is neither good nor bad news.
 */
public enum RiskLevel {

    /** Doing well against the target. */
    ON_TRACK("On track", "green"),

    /** Part-way towards a goal; no concern, but not finished either. */
    IN_PROGRESS("In progress", "blue"),

    /** Drifting away from the target in a way worth raising gently. */
    WORTH_ATTENTION("Worth attention", "amber");

    private final String status;
    private final String tone;

    RiskLevel(String status, String tone) {
        this.status = status;
        this.tone = tone;
    }

    /** Plain-language label shown on the card, e.g. {@code "Worth attention"}. */
    public String status() {
        return status;
    }

    /** Colour tone key the front end maps to its palette. */
    public String tone() {
        return tone;
    }
}
