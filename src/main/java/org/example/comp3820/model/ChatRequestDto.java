package org.example.comp3820.model;

public record ChatRequestDto(
        String patientId,
        String question
) {}