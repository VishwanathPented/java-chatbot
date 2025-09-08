💬 Java Chatbot (Spring Boot + Gemini AI)

A human-like chatbot built with Spring Boot (Java) and a simple HTML/CSS/JavaScript frontend.
The chatbot integrates with Google’s Gemini AI API and supports conversation history, dark mode, and a modern chat UI.

⸻

🚀 Features
	•	✅ Java Spring Boot backend (API + conversation handling)
	•	✅ Simple HTML/CSS/JS frontend (served from chat.html)
	•	✅ Conversation memory (remembers chat history per session)
	•	✅ Start New Chat to reset history
	•	✅ Keyboard “Enter” to send messages
	•	✅ Dark Mode enabled by default

⸻

🛠️ Tech Stack

Backend:
	•	Spring Boot 3.x
	•	WebFlux (non-blocking HTTP client)
	•	REST API endpoints
	•	In-memory session store

Frontend:
	•	Plain HTML + CSS + JavaScript (src/main/resources/static/chat.html)
	•	Minimalistic UI with Dark Mode

AI:
	•	Google Gemini API (Generative Language)

📂 Project Structure

chatbot/
 ├── src/
 │   ├── main/
 │   │   ├── java/com/example/chatbot
 │   │   │   ├── App.java
 │   │   │   ├── controller/ChatController.java
 │   │   │   └── service/GeminiService.java
 │   │   └── resources/
 │   │       ├── application.properties
 │   │       └── static/chat.html   # Frontend UI
 │
 └── README.md

 ⚙️ Setup & Run

1️⃣ Clone Repo

        git clone https://github.com/YOUR_USERNAME/java-chatbot.git
        cd java-chatbot

2️⃣ Configure Gemini API Key

Add your Gemini API key in src/main/resources/application.properties:

        gemini.api.key=YOUR_GEMINI_API_KEY

3️⃣ Run Backend

        mvn clean install
        mvn spring-boot:run

Backend runs at 👉 http://localhost:8080

4️⃣ Open Frontend

After starting the backend, open in browser: http://localhost:8080/chat.html



