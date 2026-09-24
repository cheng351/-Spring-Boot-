package org.example.comp3820.domain.model;

import java.time.LocalDate;

/**
 * One administration of a scored clinical instrument.
 *
 * @param date  when it was recorded
 * @param score the total, on that instrument's own scale
 */
public record ScoredAssessment(LocalDate date, double score) {
}
