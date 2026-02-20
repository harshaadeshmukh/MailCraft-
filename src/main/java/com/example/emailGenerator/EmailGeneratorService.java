package com.example.emailGenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class EmailGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(EmailGeneratorService.class);

    private final WebClient webClient;
    private final ApiKeyRotator apiKeyRotator;

    public EmailGeneratorService(WebClient.Builder webClientBuilder,
                                 @Value("${gemini.api.url}") String baseURL,
                                 ApiKeyRotator apiKeyRotator) {
        this.webClient     = webClientBuilder.baseUrl(baseURL).build();
        this.apiKeyRotator = apiKeyRotator;
    }
    @Cacheable(
            value = "emailReplies",
            key   = "#emailRequest.emailContent + '-' + #emailRequest.tone"
    )
    public String generateEmailReply(EmailRequest emailRequest) throws Exception {
        String prompt    = buildPrompt(emailRequest);
        int totalKeys    = apiKeyRotator.getTotalKeys();
        Exception lastEx = null;

        // Try every key before giving up
        for (int attempt = 0; attempt < totalKeys; attempt++) {
            String currentKey = apiKeyRotator.getNextKey();
            try {
                log.info("🚀 Attempt {}/{}", attempt + 1, totalKeys);
                return callGemini(prompt, currentKey);
            } catch (Exception e) {
                log.warn("❌ Key failed on attempt {}: {}", attempt + 1, e.getMessage());
                lastEx = e;
            }
        }

        throw new RuntimeException("All Gemini API keys failed.", lastEx);
    }

    private String callGemini(String prompt, String apiKey) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode part    = mapper.createObjectNode().put("text", prompt);
        ObjectNode content = mapper.createObjectNode()
                .set("parts", mapper.createArrayNode().add(part));
        ObjectNode body    = mapper.createObjectNode()
                .set("contents", mapper.createArrayNode().add(content));

        String response = webClient.post()
                .uri("/v1beta/models/gemini-2.5-flash:generateContent")
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(mapper.writeValueAsString(body))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        return root.path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply for the following email:");
        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a ").append(emailRequest.getTone()).append(" Tone.");
        }
        prompt.append("Original Email :\n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}