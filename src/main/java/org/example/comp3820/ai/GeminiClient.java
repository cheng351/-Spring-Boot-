package org.example.comp3820.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Sends a prompt to Gemini and returns the text it replies with.
 *
 * <p>Deliberately knows nothing about patients. Callers assemble their own prompt, which keeps
 * the decision about <em>what</em> is disclosed to a third party in the calling service rather
 * than buried in HTTP plumbing.
 *
 * <p>Failures are reported as {@link AiUnavailableException} instead of being swallowed into a
 * plausible-looking sentence. A caller that silently returns "unable to connect" as if it were
 * clinical guidance is indistinguishable, to the patient, from a real answer.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;

    public GeminiClient(RestTemplate restTemplate,
                        @Value("${ai.provider.api-url}") String apiUrl,
                        @Value("${ai.provider.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    /**
     * @param prompt the complete prompt, system instructions included
     * @return the model's reply text
     * @throws AiUnavailableException if the call fails or the response carries no candidate
     */
    public String complete(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        try {
            JsonNode response = restTemplate.postForObject(
                    apiUrl + "?key=" + apiKey,
                    new HttpEntity<>(body, headers),
                    JsonNode.class);

            if (response == null || !response.has("candidates")
                    || response.path("candidates").isEmpty()) {
                throw new AiUnavailableException("Gemini returned no candidates");
            }

            String text = response.path("candidates").get(0)
                    .path("content").path("parts").get(0).path("text").asText();

            if (text == null || text.isBlank()) {
                throw new AiUnavailableException("Gemini returned an empty reply");
            }
            return text;

        } catch (AiUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Gemini call failed", e);
            throw new AiUnavailableException("Could not reach the language model", e);
        }
    }
}
