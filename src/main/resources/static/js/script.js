// DOM Elements
const chatBox = document.getElementById("chat-box");
const userInput = document.getElementById("user-input");
const sendBtn = document.getElementById("send-btn");
const quotaText = document.getElementById("quota-text");
const clearBtn = document.getElementById("clear-btn");

let quotaResetMillis = null;

// Initialize
function init() {
    loadHistory();
    fetchQuotaInfo();
    userInput.focus();

    // Event Listeners
    userInput.addEventListener("keypress", (e) => {
        if (e.key === "Enter") sendMessage();
    });

    sendBtn.addEventListener("click", sendMessage);
    clearBtn.addEventListener("click", clearChat);

    // Refresh quota every minute
    setInterval(updateQuotaCountdown, 60000);
}

// Render History
function loadHistory() {
    chatBox.innerHTML = "";
    const history = JSON.parse(localStorage.getItem("chatHistory") || "[]");

    history.forEach(msg => {
        appendMessage(msg.role, msg.text, false);
    });

    scrollToBottom();
}

// Append Message to UI
function appendMessage(role, text, save = true) {
    const msgDiv = document.createElement("div");
    msgDiv.className = `message ${role}`;

    const avatar = document.createElement("div");
    avatar.className = "avatar";
    avatar.innerHTML = role === "user"
        ? '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>'
        : '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a10 10 0 1 0 10 10H12V2z"></path><path d="M12 12 2.1 10.5a10 10 0 0 0 9.9 11.5V12z"></path><path d="m12 12-4.5-9.9a10 10 0 0 1 11.5 9.9H12z"></path></svg>';

    const contentDiv = document.createElement("div");
    contentDiv.className = "message-content";

    if (role === 'user') {
        contentDiv.textContent = text;
    } else {
        // Parse Markdown for bot responses
        contentDiv.innerHTML = marked.parse(text);
        // Highlight code blocks
        contentDiv.querySelectorAll("pre code").forEach(block => hljs.highlightElement(block));
    }

    msgDiv.appendChild(avatar);
    msgDiv.appendChild(contentDiv);
    chatBox.appendChild(msgDiv);

    if (save) {
        const history = JSON.parse(localStorage.getItem("chatHistory") || "[]");
        history.push({ role, text });
        localStorage.setItem("chatHistory", JSON.stringify(history));
    }

    scrollToBottom();
}

// Send Message Flow
async function sendMessage() {
    const text = userInput.value.trim();
    if (!text) return;

    // clear input
    userInput.value = "";

    // 1. Show User Message
    appendMessage("user", text);

    // 2. Show Typing Indicator
    const typingId = showTypingIndicator();
    scrollToBottom();

    try {
        // 3. Prepare Payload (History + Message)
        const history = JSON.parse(localStorage.getItem("chatHistory") || "[]");
        // We send the history *excluding* the message we just added (if we added it already to LS? No, appendMessage adds it)
        // Actually, appendMessage DOES add it to LS at line 58.
        // We should send the history including the new message?
        // Or send history separate from the current message?
        // The backend expects: history (context) + message (current).
        // Let's filter out the very last message from history if it duplicates 'text', OR just send previous history.
        // EASIEST: Send previous history, and 'message' is the new one.

        // Let's get history BEFORE adding the new one?
        // Wait, 'appendMessage' is called before this. So 'history' in LS contains the new message.
        // We should probably just pass the whole history to the backend and let it figure it out?
        // No, the backend code adds the "current user message" manually: geminiHistory.add(... message ...).
        // So we should send history EXCLUDING the last message (which is the current one).

        const payloadHistory = history.slice(0, -1); // Exclude the just-added user message

        const response = await fetch("/api/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                message: text,
                history: payloadHistory
            })
        });

        const reply = await response.text();

        // 4. Remove Typing Indicator
        removeTypingIndicator(typingId);

        // 5. Show Bot Response
        appendMessage("bot", reply);

    } catch (error) {
        removeTypingIndicator(typingId);
        appendMessage("bot", "⚠️ Network Error: Unable to reach the server.");
        console.error(error);
    }
}

// Typing Indicator Helpers
function showTypingIndicator() {
    const id = "typing-" + Date.now();
    const msgDiv = document.createElement("div");
    msgDiv.className = "message bot";
    msgDiv.id = id;

    const avatar = document.createElement("div");
    avatar.className = "avatar";
    avatar.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a10 10 0 1 0 10 10H12V2z"></path><path d="M12 12 2.1 10.5a10 10 0 0 0 9.9 11.5V12z"></path><path d="m12 12-4.5-9.9a10 10 0 0 1 11.5 9.9H12z"></path></svg>';

    const contentDiv = document.createElement("div");
    contentDiv.className = "message-content";
    contentDiv.innerHTML = `
        <div class="typing-dots">
            <div class="dot"></div>
            <div class="dot"></div>
            <div class="dot"></div>
        </div>
    `;

    msgDiv.appendChild(avatar);
    msgDiv.appendChild(contentDiv);
    chatBox.appendChild(msgDiv);
    return id;
}

function removeTypingIndicator(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
}

function scrollToBottom() {
    chatBox.scrollTop = chatBox.scrollHeight;
}

// Clear Chat
function clearChat() {
    if (confirm("Are you sure you want to clear the conversation history?")) {
        localStorage.removeItem("chatHistory");
        chatBox.innerHTML = "";

        // Optional: Call backend to clear memory if using persistent sessions
        fetch("/api/chat/clear", { method: "POST" }).catch(console.error);
    }
}

// Quota Logic
async function fetchQuotaInfo() {
    try {
        const res = await fetch("/api/chat/quota-reset");
        const text = await res.text();

        // Parse backend response like "⏳ Daily quota resets in 5 hours 30 minutes..."
        const match = text.match(/(\d+)\s+hours?\s+(\d+)\s+minutes?/);
        if (match) {
            const hours = parseInt(match[1]);
            const minutes = parseInt(match[2]);
            const now = new Date();
            quotaResetMillis = now.getTime() + (hours * 60 + minutes) * 60 * 1000;
            updateQuotaCountdown();
        } else {
            quotaText.textContent = text.replace("⏳ ", "");
        }
    } catch (e) {
        quotaText.textContent = "Offline";
    }
}

function updateQuotaCountdown() {
    if (!quotaResetMillis) return;

    const now = Date.now();
    const diff = quotaResetMillis - now;

    if (diff <= 0) {
        quotaText.textContent = "Quota Reset!";
        quotaResetMillis = null;
        fetchQuotaInfo(); // Re-fetch
        return;
    }

    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    quotaText.textContent = `Resets in ${hours}h ${minutes}m`;
}

// Init on Load
window.onload = init;
