package me.cresterida;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.*;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@WebSocket(path="/chatbot")
@ApplicationScoped
public class ChatWebSocket {

    @Inject
    ChatbotService chatbotService;

    @Inject
    ObjectMapper objectMapper;

    private final Logger LOGGER = Logger.getLogger(ChatWebSocket.class);

    public record ChatMessage(String message) {}

    @OnOpen
    @RunOnVirtualThread
    public String onOpen() throws Exception {
        LOGGER.info("New connection");
        // Send a simple greeting without calling LLM to avoid timeout on connection
        String initialMessage = "Hello! I'm your AI assistant. Ask me anything!";
        return objectMapper.writeValueAsString(new ChatMessage(initialMessage));
    }

    @OnTextMessage
    @RunOnVirtualThread
    public String onMessage(String message) throws Exception {
        LOGGER.info("Received message: " + message);
        String responseMessage = chatbotService.chat("dummy context", message);
        return objectMapper.writeValueAsString(new ChatMessage(responseMessage));
    }
}

