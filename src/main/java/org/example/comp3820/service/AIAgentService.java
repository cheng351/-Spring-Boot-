package org.example.comp3820.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.comp3820.model.PatientProfileDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIAgentService {

    @Value("${ai.provider.api-url}")
    private String apiUrl;

    @Value("${ai.provider.api-key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    public String generatePersonalizedGuidance(PatientProfileDto profile) {
        String systemPrompt = "You are a warm and caring assistant for someone with Parkinson's. " +
                "Look at their complete health profile and give them 1 or 2 simple, everyday tips to keep their brain healthy. " +
                "CRITICAL RULES: 1. Maximum 3 sentences. 2. Use simple language. 3. NO medical jargon. 4. NO bullet points.";

        // 将 PatientProfileDto 的所有字段完整拼入
        String userPrompt = String.format(
                "Complete Patient Profile:\n- Patient ID: %s\n- Name: %s\n- Age: %d\n- Gender: %s\n- Ethnicity: %s\n- Conditions: %s\n- Observations/Risk Factors: %s",
                profile.patientId(),
                profile.name(),
                profile.age(),
                profile.gender(),
                profile.ethnicity(),
                profile.conditions().toString(),
                profile.riskFactorObservations().toString()
        );

        return callGeminiApi(systemPrompt + "\n\n" + userPrompt);
    }

    public String answerCustomQuestion(PatientProfileDto profile, String userQuestion) {
        String systemPrompt = "You are a friendly, reassuring assistant talking directly to a person with Parkinson's. " +
                "Answer their question accurately using their complete profile data provided below. " +
                "CRITICAL RULES: 1. Keep it under 4 short sentences. 2. Use simple words. 3. NO medical jargon.";

        // 将 PatientProfileDto 的所有字段完整拼入上下文
        String context = String.format(
                "Complete Patient Profile:\n- Patient ID: %s\n- Name: %s\n- Age: %d\n- Gender: %s\n- Ethnicity: %s\n- Conditions: %s\n- Observations/Risk Factors: %s",
                profile.patientId(),
                profile.name(),
                profile.age(),
                profile.gender(),
                profile.ethnicity(),
                profile.conditions().toString(),
                profile.riskFactorObservations().toString()
        );

        String fullPrompt = systemPrompt + "\n\n" + context + "\n\nPatient Question: " + userQuestion;

        return callGeminiApi(fullPrompt);
    }

    private String callGeminiApi(String fullPrompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> part = new HashMap<>();
            part.put("text", fullPrompt);
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));

            String requestUrl = apiUrl + "?key=" + apiKey;
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            JsonNode responseNode = restTemplate.postForObject(requestUrl, request, JsonNode.class);

            if (responseNode != null && responseNode.has("candidates")) {
                return responseNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            } else {
                return "Analysis is not available at the moment. Please try again later.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Unable to connect to the service. Please check your connection.";
        }
    }
}