package me.cresterida;

import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.SessionScoped;

import io.smallrye.mutiny.Uni;

@RegisterAiService
@SessionScoped
public interface ChatbotService {

    @SystemMessage("You are a bot that helps users with their queries")

    String chat(String message);
}
