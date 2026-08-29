package org.example.comp3820.controller;

import org.example.comp3820.model.GuidanceResponseDto;
import org.example.comp3820.model.PatientProfileDto;
import org.example.comp3820.service.AIAgentService;
import org.example.comp3820.service.ClinicalDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;

@RestController
@RequestMapping("/api/support")
@CrossOrigin(origins = "*") // 允许本地前端跨域调用
public class PatientSupportController {

    @Autowired
    private ClinicalDataService clinicalDataService;

    @Autowired
    private AIAgentService aiAgentService;

    @GetMapping("/guidance/{patientId}")
    public GuidanceResponseDto getDementiaRiskGuidance(@PathVariable String patientId) {
        // 1. 获取并聚合临床档案
        PatientProfileDto profile = clinicalDataService.getPatientProfile(patientId);

        // 2. 调用 AI 获取平实语言的建议
        String guidance = aiAgentService.generatePersonalizedGuidance(profile);

        // 3. 封装并返回响应
        return new GuidanceResponseDto(
                patientId,
                guidance,
                Instant.now().toString()
        );
    }


    /**
     * 专用于独立测试 AI 大模型的接口 (绕过 FHIR EHR 连接)
     */
    @PostMapping("/test-ai")
    public GuidanceResponseDto testAIAgent(@RequestBody PatientProfileDto mockProfile) {
        // 直接将接收到的模拟数据传给 AI 服务
        String guidance = aiAgentService.generatePersonalizedGuidance(mockProfile);

        return new GuidanceResponseDto(
                mockProfile.patientId(),
                guidance,
                Instant.now().toString()
        );
    }
}