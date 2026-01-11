# WebSocket Test Failed - Here's What You Need to Do

## Current Status

❌ **Quarkus backend is NOT running** (port 8082 not responding)  
❌ **Granite LLM server is NOT running** (no container found)  
❌ **WebSocket cannot connect** (no server to connect to)

---

## What You Need to Do

### Step 1: Start the Granite LLM Server

```bash
docker compose up -d granite
```

**Wait for the model to load** (this can take 30-60 seconds):

```bash
# Check if it's running
docker ps | grep granite

# Check logs to see if model is loaded
docker logs granite-cpu-server

# Test if it's responding
curl http://localhost:8080/v1/models
```

You should see output like:
```json
{
  "object": "list",
  "data": [
    {
      "id": "granite",
      ...
    }
  ]
}
```

### Step 2: Start Quarkus Backend

Open a new terminal and run:

```bash
cd /mydata/codes/2025/ai-rag-poc
./mvnw quarkus:dev
```

**Wait for the application to start.** You'll see output like:
```
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
...
Listening on: http://localhost:8082
```

Test it works:
```bash
curl http://localhost:8082/caton/hello
```

Expected: `Hello from ai-rag-poc`

### Step 3: Test the WebSocket Connection

Once both services are running, test the WebSocket:

```bash
# Using the test script
node test-websocket.js

# Or manually with curl (simpler)
curl http://localhost:8082/caton/hello
```

---

## Alternative: Test Without wscat

Since `wscat` is broken, I've created a simple Node.js test script for you.

**Usage:**
```bash
node test-websocket.js
```

This will:
1. Connect to the WebSocket
2. Receive the initial greeting from the bot
3. Send a test message "Hello"
4. Display the bot's response
5. Auto-disconnect after 10 seconds

---

## Quick Verification Checklist

Run these commands to verify everything:

```bash
# 1. Check Granite is running
docker ps | grep granite
# Should show: granite-cpu-server   Up X minutes   0.0.0.0:8080->8080/tcp

# 2. Test Granite API
curl http://localhost:8080/v1/models
# Should return JSON with model info

# 3. Check Quarkus is running
curl http://localhost:8082/caton/hello
# Should return: Hello from ai-rag-poc

# 4. Check Infinispan
docker ps | grep infinispan
# Should show infinispan container running

# 5. Test WebSocket
node test-websocket.js
# Should connect and receive messages
```

---

## Common Issues

### Issue: Granite container won't start

**Check logs:**
```bash
docker logs granite-cpu-server
```

**Common causes:**
- Model file missing in `./models/` directory
- Port 8080 already in use by another service
- Insufficient memory (needs ~8GB RAM)

**Solution:**
```bash
# Stop any service using port 8080
sudo lsof -ti:8080 | xargs kill -9

# Restart Granite
docker compose restart granite
```

### Issue: Quarkus fails to start

**Common causes:**
- Port 8082 already in use
- Maven compilation errors
- Missing frontend dependencies

**Solution:**
```bash
# Install frontend dependencies
cd src/main/webui
npm install
cd ../../..

# Clean and restart
./mvnw clean quarkus:dev
```

### Issue: WebSocket connects but bot doesn't respond

**This means:**
- ✅ Quarkus is running
- ❌ Granite LLM is not running or not configured correctly

**Check:**
```bash
# Test if Granite is responding
curl http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "granite",
    "messages": [{"role": "user", "content": "Hi"}]
  }'
```

If this fails, Granite is not working correctly.

---

## Expected Behavior When Working

### 1. Start Granite
```bash
$ docker compose up -d granite
[+] Running 1/1
 ✔ Container granite-cpu-server  Started
```

### 2. Start Quarkus
```bash
$ ./mvnw quarkus:dev
...
Listening on: http://localhost:8082
```

### 3. Test WebSocket
```bash
$ node test-websocket.js
✅ Connected to WebSocket
Waiting for initial message...

📥 Received: {"message":"Hello, how can I help you today?"}

📤 Sending test message: "Hello"

📥 Received: {"message":"Hello! I'm here to assist you..."}

🔌 Connection closed
```

### 4. Access UI
Open browser: http://localhost:8082/caton

You should see the Angular UI with a chatbot interface.

---

## Summary

**To fix the wscat error and test the chatbot:**

1. **Don't use wscat** - it's broken. Use `node test-websocket.js` instead.

2. **Start the required services:**
   ```bash
   # Terminal 1: Start Granite
   docker compose up -d granite
   
   # Terminal 2: Start Quarkus
   ./mvnw quarkus:dev
   ```

3. **Test the connection:**
   ```bash
   node test-websocket.js
   ```

4. **Access the UI:**
   ```
   http://localhost:8082/caton
   ```

---

## Files Created

- `test-websocket.js` - WebSocket test script (alternative to wscat)
- `CHATBOT-SETUP.md` - Complete setup guide
- `CHATBOT-STATUS.md` - Status check commands
- `working-steps.md` - Infinispan architecture guide

All documentation has been updated with the correct Granite port (8080).

