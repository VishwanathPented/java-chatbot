package com.example.chatbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class GeminiService {

    private final WebClient webClient;
    private final String apiKey;

    // 🔹 Session memory: sessionId -> conversation history
    private final Map<String, List<Map<String, Object>>> sessionHistories = new HashMap<>();

    public GeminiService(@Value("${gemini.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
                .build();
    }

    public String getResponse(String sessionId, String message) {
        try {
            // Get or create session history
            sessionHistories.putIfAbsent(sessionId, new ArrayList<>());
            List<Map<String, Object>> history = sessionHistories.get(sessionId);

            // Add user message
            history.add(Map.of("role", "user", "parts", List.of(Map.of("text", message))));

            // Build request body
            Map<String, Object> requestBody = Map.of("contents", history);

            // Call Gemini API
            Map<String, Object> response = webClient.post()
                    .uri("/gemini-1.5-flash:generateContent?key=" + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // Extract response text
            if (response != null && response.containsKey("candidates")) {
                var candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    var content = (Map<String, Object>) candidates.get(0).get("content");
                    var parts = (List<Map<String, Object>>) content.get("parts");
                    if (!parts.isEmpty() && parts.get(0).containsKey("text")) {
                        String reply = parts.get(0).get("text").toString();

                        // Add bot reply to history
                        history.add(Map.of("role", "model", "parts", List.of(Map.of("text", reply))));
                        return reply;
                    }
                }
            }
            return "⚠️ No response from Gemini";
        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Error: " + e.getMessage();
        }
    }

    // 🔹 Add this helper method
    public void clearSession(String sessionId) {
        sessionHistories.remove(sessionId);
    }
}
