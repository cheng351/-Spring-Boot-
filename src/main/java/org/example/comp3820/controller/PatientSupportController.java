package org.example.comp3820.controller;

import org.example.comp3820.domain.model.PatientIdentity;
import org.example.comp3820.model.ChatRequestDto;
import org.example.comp3820.model.GuidanceResponseDto;
import org.example.comp3820.support.PatientChatService;
import org.example.comp3820.support.RiskFactorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * The HTTP surface the front end talks to.
 *
 * <p>Two paths, deliberately kept apart. {@code /profile} returns identifying details for our
 * own UI to render. Everything else routes through {@link PatientChatService}, which reads a
 * de-identified snapshot — so the patient's name and identifier never reach the language model.
 * The split is the whole point; collapsing these two into one "patient" response would quietly
 * undo it.
 *
 * <p>CORS is configured centrally rather than with {@code @CrossOrigin} here, because a wildcard
 * origin and credentialed requests are mutually exclusive in the browser and the deployed front
 * end will be on a different origin.
 */
@RestController
@RequestMapping("/api/support")
public class PatientSupportController {

    private final RiskFactorService riskFactorService;
    private final PatientChatService chatService;

    public PatientSupportController(RiskFactorService riskFactorService,
                                    PatientChatService chatService) {
        this.riskFactorService = riskFactorService;
        this.chatService = chatService;
    }

    /** Identifying details for the greeting and the record drawer. Never sent to the model. */
    @GetMapping("/profile/{patientId}")
    public PatientIdentity profile(@PathVariable String patientId) {
        return riskFactorService.identityFor(patientId);
    }

    /** The opening message shown when the patient arrives. */
    @GetMapping("/guidance/{patientId}")
    public GuidanceResponseDto guidance(@PathVariable String patientId) {
        return new GuidanceResponseDto(
                patientId,
                chatService.opening(patientId),
                Instant.now().toString());
    }

    /** A free-text question about the patient's own record. */
    @PostMapping("/ask")
    public GuidanceResponseDto ask(@RequestBody ChatRequestDto request) {
        return new GuidanceResponseDto(
                request.patientId(),
                chatService.answer(request.patientId(), request.question()),
                Instant.now().toString());
    }
}
