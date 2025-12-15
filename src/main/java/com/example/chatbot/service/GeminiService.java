package com.example.chatbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeminiService {

    private final WebClient webClient;
    private final String apiKey;
    private static final String MODEL_NAME = "gemini-flash-latest";
    private static final int MAX_HISTORY = 20;

    // 🔹 Session memory: REMOVED (Stateless)

    public GeminiService(@Value("${gemini.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
                .build();
    }

    public String getResponse(List<Map<String, Object>> clientHistory, String message) {
        try {
            // Sliding Window: Keep only the last N messages to save tokens
            if (clientHistory != null && clientHistory.size() > MAX_HISTORY) {
                clientHistory = clientHistory.subList(clientHistory.size() - MAX_HISTORY, clientHistory.size());
            }

            // Prepare history for Gemini (Map 'bot' -> 'model', ensure structure)
            List<Map<String, Object>> geminiHistory = new ArrayList<>();

            if (clientHistory != null) {
                for (Map<String, Object> msg : clientHistory) {
                    String role = (String) msg.get("role");
                    String text = (String) msg.get("text");

                    // Map frontend role to API role
                    if ("bot".equals(role)) role = "model";
                    if ("error".equals(role)) continue; // skip errors

                    geminiHistory.add(Map.of("role", role, "parts", List.of(Map.of("text", text))));
                }
            }

            // Add current user message
            geminiHistory.add(Map.of("role", "user", "parts", List.of(Map.of("text", message))));

            // Build request body
            System.out.println("DEBUG: Sending context with " + geminiHistory.size() + " messages.");
            Map<String, Object> requestBody = Map.of("contents", geminiHistory);

            // Call Gemini API
            Map<String, Object> response = webClient.post()
                    .uri("/" + MODEL_NAME + ":generateContent?key=" + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // Extract response text
            String reply = extractResponseText(response);

            if (reply != null) {
                return reply;
            }

            return "⚠️ No response from Gemini";

        } catch (WebClientResponseException.TooManyRequests e) {
            return "⚠️ You’ve hit the request limit. Please wait and try again.";
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            System.err.println("❌ Gemini API Error: " + e.getStatusCode() + " - " + responseBody);
            return "⚠️ Gemini API error: " + e.getStatusCode() + " (Check logs for details)";
        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Unexpected error: " + e.getMessage();
        }
    }

    private String extractResponseText(Map<String, Object> response) {
        if (response != null && response.containsKey("candidates")) {
            var candidates = (List<Map<String, Object>>) response.get("candidates");
            if (!candidates.isEmpty()) {
                var content = (Map<String, Object>) candidates.get(0).get("content");
                var parts = (List<Map<String, Object>>) content.get("parts");
                if (!parts.isEmpty() && parts.get(0).containsKey("text")) {
                    return parts.get(0).get("text").toString();
                }
            }
        }
        return null;
    }

    // 🔹 Helper: Clear session (No-op in stateless)
    public void clearSession(String sessionId) {
        // Stateless, nothing to clear on server
    }

    // 🔹 Helper: Get quota reset countdown
    public String getQuotaResetTime() {
        ZoneId pacificZone = ZoneId.of("America/Los_Angeles");
        ZonedDateTime nowPacific = ZonedDateTime.now(pacificZone);
        ZonedDateTime midnightPacific = nowPacific.toLocalDate().plusDays(1).atStartOfDay(pacificZone);

        Duration untilReset = Duration.between(nowPacific, midnightPacific);

        long hours = untilReset.toHours();
        long minutes = untilReset.toMinutesPart();

        return String.format("⏳ Daily quota resets in %d hours %d minutes (midnight PT)", hours, minutes);
    }
}
