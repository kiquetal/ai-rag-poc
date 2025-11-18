package me.cresterida;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.websockets.next.*;
import io.vertx.ext.web.Session;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@WebSocket(path="/chatbot")
@ApplicationScoped
public class ChatWebSocket {

    @Inject
    ChatbotService chatbotService;


    @OnOpen
    public String onOpen()
    {
        return chatbotService.chat("Hello, how can i help you");
    }
    @OnTextMessage
    public void onMessage(String message) {
        String response = chatbotService.chat(message);

    }
}

