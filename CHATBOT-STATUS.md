# Chatbot Status - Quick Check

**Generated:** January 11, 2026

## Current Configuration

Your chatbot is configured to use a **local LLM server** at:
```
http://localhost:46383/v1
```

## Status Check Results

### ✅ What's Running:
- **Infinispan**: Running on port 32771
- **Whisper Server**: Running on port 8181
- **Quarkus Dev**: Should be running on port 8082

### ❌ What's Missing:
- **Granite LLM Server**: NOT running on port 46383
  - The chatbot **will NOT work** until this is started

## How to Fix

### Option 1: Start Granite via Docker Compose (Recommended)

```bash
# Start the Granite LLM server
docker compose up -d granite

# Wait a few seconds, then verify it's running
curl http://localhost:46383/v1/models

# You should see a JSON response listing available models
```

### Option 2: Check if Granite is on a Different Port

```bash
# Check if llama-server or granite is running on port 8080
curl http://localhost:8080/v1/models

# If this works, update application.properties:
# Change: quarkus.langchain4j.openai.base-url=http://localhost:8080/v1
```

### Option 3: Use Gemini Instead (Cloud-Based)

If you don't want to run a local LLM:

1. Get Gemini API credentials from Google Cloud
2. Set environment variables:
   ```bash
   export GEMINI_API_KEY="your-api-key"
   export GEMINI_PROJECT_ID="your-project-id"
   ```

3. Edit `src/main/resources/application.properties`:
   ```properties
   # Comment out or change:
   # quarkus.langchain4j.chat-model.provider=openai
   
   # To:
   quarkus.langchain4j.chat-model.provider=gemini
   ```

4. Restart Quarkus: `./mvnw quarkus:dev`

## Quick Verification Commands

```bash
# 1. Check if Quarkus is running
curl http://localhost:8082/caton/hello

# 2. Check if LLM server is running  
curl http://localhost:46383/v1/models

# 3. Check if Infinispan is accessible
INFINISPAN_PORT=$(docker port $(docker ps -q --filter "ancestor=quay.io/infinispan/server:15.0") 11222 | cut -d: -f2)
curl http://localhost:$INFINISPAN_PORT/rest/v2/caches

# 4. Test WebSocket (requires wscat: npm install -g wscat)
wscat -c ws://localhost:8082/caton/chatbot
```

## What Happens When You Access the Chatbot Now

Since the Granite server is not running:

1. ✅ WebSocket will connect successfully
2. ✅ You can type messages
3. ❌ **Bot will not respond** (or will timeout after 80 seconds)
4. ❌ You'll see connection errors in Quarkus logs

## Action Required

**To enable the chatbot, you MUST start the Granite LLM server:**

```bash
docker compose up -d granite
```

Then verify:
```bash
curl http://localhost:46383/v1/models
```

Expected output:
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

Once the LLM server is running, the chatbot will work immediately (no need to restart Quarkus in dev mode).

---

## Summary

**To enable chatbot capability, you need:**

1. ✅ **Quarkus running** (`./mvnw quarkus:dev`)
2. ✅ **Infinispan running** (automatic in dev mode) 
3. ✅ **Frontend built** (`npm install` in webui folder)
4. ❌ **LLM server running** ← **THIS IS MISSING**

**Quick fix:**
```bash
docker compose up -d granite
```

See `CHATBOT-SETUP.md` for detailed setup instructions.

