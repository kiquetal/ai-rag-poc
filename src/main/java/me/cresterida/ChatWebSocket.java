package me.cresterida;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private Logger LOGGER = Logger.getLogger(ChatWebSocket.class);



    @OnOpen
    public String onOpen()
    {
        LOGGER.info("New connection");
        return chatbotService.chat("Hello, how can i help you");
    }
    @OnTextMessage
    public String onMessage(String message) {
        LOGGER.info("Received message");
        return chatbotService.chat(message);
    }
}

