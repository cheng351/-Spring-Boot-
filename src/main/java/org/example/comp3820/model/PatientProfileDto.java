package org.example.comp3820.model;

import java.util.List;

public record PatientProfileDto(
        String patientId,
        String name,
        int age,
        String gender,
        List<String> conditions,
        List<String> riskFactorObservations
) {}