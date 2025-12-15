package com.example.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @org.springframework.beans.factory.annotation.Value("${gemini.api.key}")
    private String apiKey;

    @javax.annotation.PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("❌ API Key is missing! Please set gemini.api.key in application.properties");
        } else {
            System.out.println("✅ API Key loaded: " + apiKey.substring(0, 5) + "...");
        }
    }
}
