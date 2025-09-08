package com.example.chatbot.controller;

import com.example.chatbot.service.GeminiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final GeminiService geminiService;

    public ChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @GetMapping
    public String chat(
            @RequestParam String msg,
            @RequestParam(defaultValue = "default") String sessionId
    ) {
        // Call GeminiService with sessionId + user message
        return geminiService.getResponse(sessionId, msg);
    }

    @PostMapping("/clear")
    public String clearHistory(@RequestParam(defaultValue = "default") String sessionId) {
        // Clear memory inside GeminiService
        geminiService.clearSession(sessionId);
        return "✅ Chat history cleared for session: " + sessionId;
    }

    @GetMapping("/quota-reset")
    public String getQuotaReset() {
        return geminiService.getQuotaResetTime();
    }
}
