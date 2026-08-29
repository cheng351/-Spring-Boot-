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
        // 1. 构建 Prompt，强调为帕金森病患者提供通俗易懂的建议
        String systemPrompt = "You are a specialized AI assistant supporting Parkinson's disease patients. " +
                "Review the provided clinical data (conditions and lifestyle observations). " +
                "Generate a short, plain-language summary addressing modifiable dementia risk factors " +
                "(e.g., sleep, physical activity). Avoid complex medical jargon.";

        String userPrompt = String.format("Patient Data: Conditions: %s, Observations: %s",
                profile.conditions().toString(), profile.riskFactorObservations().toString());

        // 为了简化向 Gemini API 发送的结构，我们可以将 System Prompt 和 User Prompt 组合起来
        String fullPrompt = systemPrompt + "\n\n" + userPrompt;

        try {
            // 2. 设置请求头 (Gemini 不需要 Bearer Auth，Key 放在 URL 中)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 3. 构造 Gemini 要求的标准 JSON 请求体
            // 对应结构: { "contents": [ { "parts": [ { "text": "你的 Prompt" } ] } ] }
            Map<String, Object> part = new HashMap<>();
            part.put("text", fullPrompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));

            // 4. 将 API Key 拼接到 URL 后面
            String requestUrl = apiUrl + "?key=" + apiKey;

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 5. 发送请求并使用 JsonNode 解析响应
            JsonNode responseNode = restTemplate.postForObject(requestUrl, request, JsonNode.class);

            // 6. 安全地提取生成的文本内容
            if (responseNode != null && responseNode.has("candidates")) {
                return responseNode
                        .path("candidates")
                        .get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText();
            } else {
                return "分析暂不可用：AI 模型未返回有效结果。";
            }

        } catch (Exception e) {
            // 捕获异常防止服务崩溃，并将错误输出到控制台方便调试
            e.printStackTrace();
            return "无法连接到 AI 服务，请检查网络或 API 配置。错误信息: " + e.getMessage();
        }
    }
}