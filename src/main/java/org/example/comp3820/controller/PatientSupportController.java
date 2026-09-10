package org.example.comp3820.controller;

import org.example.comp3820.model.ChatRequestDto;
import org.example.comp3820.model.GuidanceResponseDto;
import org.example.comp3820.model.PatientProfileDto;
import org.example.comp3820.service.AIAgentService;
import org.example.comp3820.service.ClinicalDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/support")
@CrossOrigin(origins = "*")
public class PatientSupportController {

    @Autowired
    private ClinicalDataService clinicalDataService;

    @Autowired
    private AIAgentService aiAgentService;

    @GetMapping("/guidance/{patientId}")
    public GuidanceResponseDto getDementiaRiskGuidance(@PathVariable String patientId) {
        PatientProfileDto profile = clinicalDataService.getPatientProfile(patientId);
        String guidance = aiAgentService.generatePersonalizedGuidance(profile);
        return new GuidanceResponseDto(patientId, guidance, Instant.now().toString());
    }

    @PostMapping("/ask")
    public GuidanceResponseDto askCustomQuestion(@RequestBody ChatRequestDto request) {
        PatientProfileDto profile = clinicalDataService.getPatientProfile(request.patientId());
        String aiAnswer = aiAgentService.answerCustomQuestion(profile, request.question());
        return new GuidanceResponseDto(request.patientId(), aiAnswer, Instant.now().toString());
    }

    @PostMapping("/test-ai")
    public GuidanceResponseDto testAIAgent(@RequestBody PatientProfileDto mockProfile) {
        String guidance = aiAgentService.generatePersonalizedGuidance(mockProfile);
        return new GuidanceResponseDto(mockProfile.patientId(), guidance, Instant.now().toString());
    }
}