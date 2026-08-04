/**
 * ChatbotService
 *
 * PURPOSE: Answers free-text questions about the fraud-detection system by
 *          grounding a Groq chat completion (OpenAI-compatible API) in two
 *          sources ONLY:
 *            1. The live 'rules' table (via RuleRepository) - current rule
 *               config (active/weight/threshold/timeline).
 *            2. Local knowledge text files (RiskLogic.md, Individual-Rules.md)
 *               describing how the risk engine works.
 *
 * NO WEB SEARCH: The system prompt explicitly restricts the model to the
 *                supplied context and forbids using outside/web knowledge.
 *                If the answer isn't in the context, it must say so.
 */
package com.example.service;

import com.example.entity.Rule;
import com.example.repository.RuleRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final String SYSTEM_PROMPT = """
            You are a fraud-detection assistant for the Sentinel system.
            Answer questions ONLY using the CONTEXT provided below (current rule
            configuration from the database, and the risk-engine documentation).
            Do not use any outside knowledge, do not search the web, and do not
            invent information. If the answer cannot be found in the CONTEXT,
            reply exactly: "I don't have enough information in the rule
            database or documentation to answer that."
            """;

    private final RuleRepository ruleRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model}")
    private String model;

    @Value("${chatbot.knowledge-files}")
    private String knowledgeFilesConfig;

    private String knowledgeText = "";

    public ChatbotService(RuleRepository ruleRepository, ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadKnowledgeFiles() {
        StringBuilder combined = new StringBuilder();
        for (String rawPath : knowledgeFilesConfig.split(",")) {
            String pathStr = rawPath.trim();
            if (pathStr.isEmpty()) {
                continue;
            }
            try {
                Path path = Path.of(pathStr);
                if (Files.exists(path)) {
                    combined.append("## Source: ").append(pathStr).append("\n")
                            .append(Files.readString(path, StandardCharsets.UTF_8))
                            .append("\n\n");
                } else {
                    combined.append("## Source: ").append(pathStr)
                            .append(" (not found - skipped)\n\n");
                }
            } catch (IOException e) {
                combined.append("## Source: ").append(pathStr)
                        .append(" (failed to read: ").append(e.getMessage()).append(")\n\n");
            }
        }
        this.knowledgeText = combined.toString();
    }

    public String askQuestion(String question) {
        if (apiKey == null || apiKey.isBlank()) {
            return "Chatbot is not configured: set the GROQ_API_KEY environment variable and restart the app.";
        }

        String context = buildContext();
        String userMessage = "CONTEXT:\n" + context + "\n\nQUESTION:\n" + question;

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userMessage)
                    )
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Groq API error (" + response.statusCode() + "): " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unexpected Groq response: " + response.body());
            }
            return jsonText(content);
        } catch (IOException | InterruptedException | JacksonException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to reach Groq API: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("deprecation")
    private static String jsonText(JsonNode node) {
        return node.asText();
    }

    private String buildContext() {
        List<Rule> rules = ruleRepository.findAll();
        String rulesSection = rules.stream()
                .map(r -> "- %s (%s): active=%s, weight=%s, threshold=%s, timeline=%dd".formatted(
                        r.getRuleName(), r.getRuleType(), r.isActive(), r.getWeight(), r.getThresholdValue(), r.getTimeline()))
                .collect(Collectors.joining("\n"));

        return "### Current rules (from database)\n" + rulesSection
                + "\n\n### Risk engine documentation\n" + knowledgeText;
    }
}
