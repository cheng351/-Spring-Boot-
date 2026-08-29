package org.example.comp3820.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.example.comp3820.model.PatientProfileDto;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClinicalDataService {

    @Autowired
    private IGenericClient fhirClient;

    public PatientProfileDto getPatientProfile(String patientId) {
        // 1. 获取患者基本信息
        Patient patient = fhirClient.read().resource(Patient.class).withId(patientId).execute();
        String name = patient.getNameFirstRep().getNameAsSingleString();
        String gender = patient.getGender() != null ? patient.getGender().getDisplay() : "Unknown";

        // 2. 获取诊断条件 (Condition)
        Bundle conditionBundle = fhirClient.search().forResource(Condition.class)
                .where(Condition.PATIENT.hasId(patientId))
                .returnBundle(Bundle.class)
                .execute();
        List<String> conditions = conditionBundle.getEntry().stream()
                .map(entry -> ((Condition) entry.getResource()).getCode().getText())
                .collect(Collectors.toList());

        // 3. 获取风险因素观察指标 (Observation - 例如睡眠或运动)
        Bundle observationBundle = fhirClient.search().forResource(Observation.class)
                .where(Observation.PATIENT.hasId(patientId))
                .returnBundle(Bundle.class)
                .execute();
        List<String> observations = observationBundle.getEntry().stream()
                .map(entry -> ((Observation) entry.getResource()).getCode().getText())
                .collect(Collectors.toList());

        // 4. 组装并返回 DTO
        return new PatientProfileDto(patientId, name, 65, gender, conditions, observations);
    }
}