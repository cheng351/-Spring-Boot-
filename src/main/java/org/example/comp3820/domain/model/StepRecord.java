package org.example.comp3820.domain.model;

import java.time.LocalDate;

/**
 * One day of walking, derived from a FHIR Observation coded
 * LOINC 41950-7 (Number of steps in 24 hour Measured).
 *
 * @param date  the day the steps were counted
 * @param steps step count for that day
 */
public record StepRecord(LocalDate date, int steps) {
}
