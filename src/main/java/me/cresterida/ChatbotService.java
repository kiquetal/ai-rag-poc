package me.cresterida;


import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.SessionScoped;

@RegisterAiService
@SessionScoped
public interface ChatbotService {


    @UserMessage("""
            Context: {context}
            User: {message}
            """)
    String chat(String context, String message);
}
