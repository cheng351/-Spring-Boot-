package org.example.comp3820.domain.model;

import java.time.LocalDate;

/**
 * One night of sleep, derived from a FHIR Observation coded LOINC 93832-4 (Sleep duration).
 *
 * @param date  the night the sleep was recorded against
 * @param hours total sleep duration in hours
 */
public record SleepRecord(LocalDate date, double hours) {
}
