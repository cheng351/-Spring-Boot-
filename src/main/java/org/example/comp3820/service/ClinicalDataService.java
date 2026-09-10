package org.example.comp3820.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.example.comp3820.model.PatientProfileDto;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClinicalDataService {

    @Autowired
    private IGenericClient fhirClient;

    public PatientProfileDto getPatientProfile(String patientId) {
        // 1. 获取基本患者信息
        Patient patient = fhirClient.read().resource(Patient.class).withId(patientId).execute();

        String name = "Unknown";
        if (patient.hasName() && !patient.getName().isEmpty()) {
            name = patient.getNameFirstRep().getNameAsSingleString();
        }

        String gender = patient.getGender() != null ? patient.getGender().getDisplay() : "Unknown";

        // 动态计算年龄
        int age = 0;
        if (patient.hasBirthDate()) {
            int birthYear = patient.getBirthDate().getYear() + 1900;
            age = LocalDate.now().getYear() - birthYear;
        }

        // 解析种族 (Ethnicity) 恢复你最开始的需求
        String ethnicity = "Unknown";
        Extension ethExt = patient.getExtensionByUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity");
        if (ethExt != null) {
            Extension textExt = ethExt.getExtensionByUrl("text");
            if (textExt != null && textExt.hasValue()) {
                ethnicity = textExt.getValue().toString();
            }
        }

        // 2. 获取疾病史 (Condition)
        Bundle conditionBundle = fhirClient.search().forResource(Condition.class)
                .where(Condition.PATIENT.hasId(patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<String> conditions = conditionBundle.getEntry().stream()
                .map(entry -> (Condition) entry.getResource())
                .filter(c -> c.hasCode() && c.getCode().hasText())
                .map(c -> c.getCode().getText())
                .distinct()
                .collect(Collectors.toList());

        // 3. 获取风险因素观察指标 (Observation) - 包含具体分数
        Bundle observationBundle = fhirClient.search().forResource(Observation.class)
                .where(Observation.PATIENT.hasId(patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<String> observations = observationBundle.getEntry().stream()
                .map(entry -> (Observation) entry.getResource())
                .filter(o -> o.hasCode() && o.getCode().hasText())
                .map(o -> {
                    String obsName = o.getCode().getText();
                    StringBuilder obsValue = new StringBuilder();

                    if (o.hasValueQuantity()) {
                        obsValue.append(o.getValueQuantity().getValue().toString());
                    } else if (o.hasValueCodeableConcept() && o.getValueCodeableConcept().hasText()) {
                        obsValue.append(o.getValueCodeableConcept().getText());
                    }
                    return obsValue.length() > 0 ? obsName + ": " + obsValue.toString() : obsName;
                })
                .distinct()
                .collect(Collectors.toList());

        return new PatientProfileDto(patientId, name, age, gender, ethnicity, conditions, observations);
    }
}