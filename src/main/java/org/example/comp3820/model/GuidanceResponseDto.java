package org.example.comp3820.model;

public record GuidanceResponseDto(
        String patientId,
        String plainLanguageGuidance,
        String generatedAt
) {}