package com.example.chatbot.controller;

import com.example.chatbot.service.GeminiService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final GeminiService geminiService;

    public ChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping
    public String chat(@RequestBody Map<String, Object> payload) {
        String msg = (String) payload.get("message");
        List<Map<String, Object>> history = (List<Map<String, Object>>) payload.get("history");

        // Call GeminiService with history + user message
        return geminiService.getResponse(history, msg);
    }

    @PostMapping("/clear")
    public String clearHistory() {
        return "✅ Chat history cleared (Client-side handled)";
    }

    @GetMapping("/quota-reset")
    public String getQuotaReset() {
        return geminiService.getQuotaResetTime();
    }
}
