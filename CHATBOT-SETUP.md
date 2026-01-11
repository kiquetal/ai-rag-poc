# Chatbot Capability Setup Guide

## Overview

This project includes a **WebSocket-based chatbot** that uses LangChain4j to interact with various LLM providers. The chatbot is accessible through a WebSocket endpoint and an Angular UI.

---

## Current Architecture

### Components

1. **ChatbotService** (`ChatbotService.java`)
   - LangChain4j AI service interface
   - Decorated with `@RegisterAiService` for automatic LLM integration
   - Session-scoped to maintain conversation context
   - Template: Sends `context` + `user message` to the LLM

2. **ChatWebSocket** (`ChatWebSocket.java`)
   - WebSocket endpoint at `/caton/chatbot`
   - Handles real-time bidirectional communication
   - Connects Angular UI to the ChatbotService

3. **Angular UI** (`chatbot.component.ts`)
   - WebSocket client that connects to `ws://localhost:8082/caton/chatbot`
   - Real-time chat interface with message history
   - Sends user messages and displays bot responses

### Data Flow

```
User → Angular UI → WebSocket → ChatWebSocket → ChatbotService → LLM Provider
                                                                        ↓
User ← Angular UI ← WebSocket ← ChatWebSocket ← ChatbotService ← LLM Response
```

---

## Configuration Options

The chatbot currently supports **two LLM provider options**:

### Option 1: Local LLM (Granite Model) - **RECOMMENDED FOR DEVELOPMENT**

✅ **Currently Configured** - This is what your `application.properties` is using.

```properties
quarkus.langchain4j.openai.base-url=http://localhost:46383/v1
quarkus.langchain4j.openai.timeout=80s
quarkus.langchain4j.chat-model.provider=openai
```

**How it works:**
- Uses OpenAI-compatible API from your local Granite model server
- The URL `http://localhost:46383/v1` should point to your local LLM server
- No API key required (or use a dummy value)
- Runs completely offline

**Check if it's running:**
```bash
curl http://localhost:46383/v1/models
```

### Option 2: Google Gemini (Cloud Service)

Configure these environment variables:

```properties
# In application.properties (already configured)
quarkus.langchain4j.gemini.api-key=${GEMINI_API_KEY:000000000000000000000000}
quarkus.langchain4j.gemini.project-id=${GEMINI_PROJECT_ID:000000000000000000000000}
quarkus.langchain4j.gemini.model-name=gemini-1.5-flash
quarkus.langchain4j.gemini.location=us-central1

# Change the provider to use Gemini
quarkus.langchain4j.chat-model.provider=gemini
```

**Set environment variables:**
```bash
export GEMINI_API_KEY="your-actual-api-key"
export GEMINI_PROJECT_ID="your-gcp-project-id"
```

---

## Quick Start Guide

### Prerequisites

1. **Java 21** and **Maven** installed
2. **Node.js** (v22.14.0 recommended) and **npm** (v10.9.2 recommended)
3. **Infinispan** running (started automatically by Quarkus Dev Services)
4. **LLM Provider** configured (choose one):
   - Local Granite server running on port 46383, OR
   - Gemini API credentials

### Step 1: Check Your LLM Provider

#### For Local LLM (Granite):

```bash
# Check if the local LLM server is running
curl http://localhost:46383/v1/models

# If not running, start the Granite container from docker-compose:
docker compose up -d granite
```

**Expected response:**
```json
{
  "object": "list",
  "data": [
    {
      "id": "granite",
      "object": "model",
      ...
    }
  ]
}
```

#### For Gemini (Cloud):

```bash
# Set environment variables
export GEMINI_API_KEY="AIza..."
export GEMINI_PROJECT_ID="my-project"

# Edit application.properties and change:
# quarkus.langchain4j.chat-model.provider=gemini
```

### Step 2: Install Frontend Dependencies

```bash
cd src/main/webui
npm install
cd ../../..
```

### Step 3: Start the Application

```bash
./mvnw quarkus:dev
```

This single command will:
- Start Quarkus backend on port 8082
- Start Infinispan Dev Services (dynamically assigned port)
- Start Angular dev server (automatically via Quinoa)
- Enable hot reload for both backend and frontend

### Step 4: Access the Chatbot

Open your browser and navigate to:

```
http://localhost:8082/caton
```

You should see the Angular application with the chatbot interface.

### Step 5: Test the WebSocket Connection

The chatbot connects via WebSocket. You can test it manually:

```bash
# Install wscat if you don't have it
npm install -g wscat

# Connect to the chatbot WebSocket
wscat -c ws://localhost:8082/caton/chatbot

# You should immediately receive a greeting message
# Type a message and press Enter to chat
```

---

## Troubleshooting

### Issue: "Connection Failed" or WebSocket Error

**Symptoms:** Angular UI shows "Bot: Connection failed" or console shows WebSocket errors.

**Solutions:**

1. **Check if Quarkus is running:**
   ```bash
   curl http://localhost:8082/caton/hello
   ```
   Should return: `Hello from ai-rag-poc`

2. **Check WebSocket endpoint:**
   ```bash
   wscat -c ws://localhost:8082/caton/chatbot
   ```
   Should immediately send a greeting message.

3. **Check browser console** (F12 → Console tab):
   - Look for WebSocket connection errors
   - Verify the URL is correct: `ws://localhost:8082/caton/chatbot`

### Issue: "Dummy context" Response

**Symptoms:** Chatbot responds but seems limited or uses placeholder context.

**Cause:** The current implementation uses a hardcoded "dummy context" string.

**Solution:** Integrate with RAG (Retrieval-Augmented Generation):

The chatbot should retrieve relevant context from the vector store before generating responses. See the section below on "Integrating RAG with Chatbot".

### Issue: LLM Connection Timeout

**Symptoms:** Chatbot takes very long to respond or times out.

**Solutions:**

1. **For local LLM:**
   - Verify Granite server is running: `docker ps | grep granite`
   - Check if port 46383 is correct: `docker port <granite-container-id>`
   - Increase timeout in `application.properties`:
     ```properties
     quarkus.langchain4j.openai.timeout=120s
     ```

2. **For Gemini:**
   - Verify API key is valid
   - Check network connectivity
   - Verify project ID is correct

### Issue: No Response from Chatbot

**Symptoms:** User sends message, but bot never responds.

**Debug steps:**

1. **Check Quarkus logs:**
   ```bash
   # Look for errors in the terminal where you ran ./mvnw quarkus:dev
   ```

2. **Check if ChatbotService is initialized:**
   ```bash
   # Look for "New connection" in logs when opening the UI
   ```

3. **Test the LLM provider directly:**
   ```bash
   # For local LLM:
   curl http://localhost:46383/v1/chat/completions \
     -H "Content-Type: application/json" \
     -d '{
       "model": "granite",
       "messages": [{"role": "user", "content": "Hello"}]
     }'
   ```

---

## Integrating RAG with Chatbot

Currently, the chatbot uses a **dummy context**. To enable true RAG functionality:

### What Needs to Change

In `ChatWebSocket.java`, the current implementation is:

```java
@OnTextMessage
public String onMessage(String message) throws Exception {
    LOGGER.info("Received message");
    String responseMessage = chatbotService.chat("dummy context", message);
    return objectMapper.writeValueAsString(new ChatMessage(responseMessage));
}
```

### Enhanced RAG Implementation

You need to:

1. **Inject the Ingestor** service
2. **Retrieve relevant documents** based on the user's message
3. **Build context** from the retrieved documents
4. **Pass real context** to the ChatbotService

Here's the enhanced version:

```java
@WebSocket(path="/chatbot")
@ApplicationScoped
public class ChatWebSocket {

    @Inject
    ChatbotService chatbotService;

    @Inject
    Ingestor ingestor;  // Add this

    @Inject
    ObjectMapper objectMapper;

    private Logger LOGGER = Logger.getLogger(ChatWebSocket.class);

    public record ChatMessage(String message) {}

    @OnOpen
    public String onOpen() throws Exception {
        LOGGER.info("New connection");
        String initialMessage = chatbotService.chat("", "Hello, how can I help you?");
        return objectMapper.writeValueAsString(new ChatMessage(initialMessage));
    }
    
    @OnTextMessage
    public String onMessage(String message) throws Exception {
        LOGGER.info("Received message: " + message);
        
        // 1. Search for relevant documents
        List<Ingestor.CombinedSearchResult> relevantDocs = 
            ingestor.findSimilarContent(message);
        
        // 2. Build context from top results
        String context = relevantDocs.stream()
            .limit(3)  // Use top 3 results
            .map(result -> 
                "Document: " + result.title() + "\n" +
                "Content: " + result.text() + "\n" +
                "Relevance: " + String.format("%.2f", result.score())
            )
            .collect(Collectors.joining("\n\n---\n\n"));
        
        // 3. If no relevant context found, provide fallback
        if (context.isEmpty()) {
            context = "No relevant documents found in the knowledge base.";
        }
        
        // 4. Send context + message to LLM
        String responseMessage = chatbotService.chat(context, message);
        return objectMapper.writeValueAsString(new ChatMessage(responseMessage));
    }
}
```

### Benefits of RAG Integration

- **Grounded responses**: Chatbot answers based on your ingested documents
- **Source attribution**: Can include document titles and relevance scores
- **Dynamic knowledge**: Responses update as you add new documents
- **Reduces hallucination**: LLM has specific context to work with

---

## Testing the Chatbot

### 1. Ingest Sample Documents

```bash
# Ingest a test document
curl -X POST http://localhost:8082/caton/documents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Infinispan Overview",
    "text": "Infinispan is a distributed in-memory key-value data store. It provides fast access to data with features like clustering, caching, and querying capabilities."
  }'

# Verify it was stored
curl http://localhost:8082/caton/documents/count
```

### 2. Test Vector Search

```bash
curl "http://localhost:8082/caton/documents/search?term=distributed%20cache"
```

### 3. Test Chatbot with RAG

Open the chatbot UI: http://localhost:8082/caton

Try questions like:
- "What is Infinispan?"
- "Tell me about distributed caching"
- "How does the data store work?"

The chatbot should respond using information from your ingested documents.

---

## Advanced Configuration

### Customizing the ChatbotService

You can enhance the prompt template in `ChatbotService.java`:

```java
@UserMessage("""
    You are a helpful AI assistant with access to a knowledge base.
    
    Context from knowledge base:
    {context}
    
    User question: {message}
    
    Instructions:
    - Answer based primarily on the provided context
    - If the context doesn't contain relevant information, say so
    - Be concise and accurate
    - Include document titles when citing sources
    """)
String chat(String context, String message);
```

### Adding Conversation Memory

For multi-turn conversations, you can add memory:

```java
@RegisterAiService
@SessionScoped  // Already session-scoped
public interface ChatbotService {

    @SystemMessage("You are a helpful AI assistant.")
    @UserMessage("""
        Context: {context}
        User: {message}
        """)
    @MemoryId  // Add this to enable conversation memory
    String chat(String context, String message);
}
```

Then configure memory in `application.properties`:

```properties
# Use in-memory chat memory (for development)
quarkus.langchain4j.chat-memory.memory-type=in-memory
quarkus.langchain4j.chat-memory.max-messages=10
```

---

## Production Considerations

### 1. Environment Variables

Never hardcode API keys. Always use environment variables:

```bash
# Production deployment
export GEMINI_API_KEY="actual-key"
export INFINISPAN_HOSTS="infinispan-cluster:11222"
export INFINISPAN_USER="admin"
export INFINISPAN_PASS="secure-password"
```

### 2. WebSocket Security

For production, secure the WebSocket endpoint:

```java
@WebSocket(path="/chatbot")
@Authenticated  // Add authentication
@ApplicationScoped
public class ChatWebSocket {
    // ... implementation
}
```

### 3. Rate Limiting

Add rate limiting to prevent abuse:

```properties
quarkus.rate-limiter.chatbot.rate-limit=10
quarkus.rate-limiter.chatbot.time-window=1m
```

### 4. Error Handling

Enhance error handling in the WebSocket:

```java
@OnError
public void onError(Throwable error) {
    LOGGER.error("WebSocket error", error);
    // Optionally send error message to client
}

@OnClose
public void onClose() {
    LOGGER.info("Connection closed");
}
```

---

## Summary

### What You Need to Enable the Chatbot:

✅ **Already Implemented:**
- WebSocket endpoint (`/caton/chatbot`)
- ChatbotService with LangChain4j integration
- Angular UI component
- LLM provider configuration (local Granite)

✅ **Just Need to Start:**
1. Ensure Granite server is running: `docker compose up -d granite`
2. Run dev mode: `./mvnw quarkus:dev`
3. Open browser: `http://localhost:8082/caton`

🔧 **Optional Enhancements:**
- Integrate RAG context retrieval (recommended)
- Add conversation memory
- Enhance prompt template
- Add authentication and rate limiting

### Quick Verification Checklist:

- [ ] Local LLM server is running (port 46383)
- [ ] Infinispan is running (automatic in dev mode)
- [ ] Frontend dependencies installed (`npm install`)
- [ ] Application started (`./mvnw quarkus:dev`)
- [ ] WebSocket connects successfully
- [ ] Chatbot responds to messages

---

## Next Steps

1. **Test the current setup** - Verify basic chatbot functionality
2. **Integrate RAG** - Implement the enhanced `ChatWebSocket` with document retrieval
3. **Add memory** - Enable conversation history for multi-turn dialogs
4. **Customize prompts** - Tailor the AI's behavior to your use case
5. **Add authentication** - Secure the endpoint for production use

For questions or issues, check the logs with `./mvnw quarkus:dev` and review the console output.

