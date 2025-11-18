package me.cresterida;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.*;
import io.vertx.ext.web.Session;
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

    private Logger LOGGER = Logger.getLogger(ChatWebSocket.class);

    public record ChatMessage(String message) {}

    @OnOpen
    public String onOpen() throws Exception {
        LOGGER.info("New connection");
        String initialMessage = chatbotService.chat("dummy context", "Hello, how can i help you");
        return objectMapper.writeValueAsString(new ChatMessage(initialMessage));
    }
    @OnTextMessage
    public String onMessage(String message) throws Exception {
        LOGGER.info("Received message");
        String responseMessage = chatbotService.chat("dummy context", message);
        return objectMapper.writeValueAsString(new ChatMessage(responseMessage));
    }
}

