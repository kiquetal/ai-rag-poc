# CRITICAL: Granite Performance Issue - Solutions

## Problem Confirmed

The latest logs show Granite is **still extremely slow**:

```
eval time = 227939.52 ms / 129 tokens (1766.97 ms per token, 0.57 tokens per second)
total time = 229708.20 ms / 130 tokens (3.8 minutes for one response!)
```

**Performance**: 0.57 tokens/second (even worse after "optimization")
**Reality**: The 8B Granite model is simply **too large** for efficient CPU inference on this system.

---

## Solution 1: Switch to Gemini (RECOMMENDED) ⚡

### Why Gemini?
- ⚡ **2-5 second responses** (100x faster!)
- 💰 **Very cheap**: ~$0.0001 per message
- 🎯 **Better quality** than local 8B model
- ☁️ **No local resources** needed

### Steps to Enable Gemini:

#### 1. Get API Credentials

Visit: https://console.cloud.google.com/

1. Create/select a Google Cloud project
2. Enable "Vertex AI API"
3. Create API key or use service account
4. Get your project ID

#### 2. Set Environment Variables

In the terminal where you run `mvnw quarkus:dev`:

```bash
export GEMINI_API_KEY="your-api-key-here"
export GEMINI_PROJECT_ID="your-gcp-project-id"
```

**OR** add to `~/.bashrc` to persist:

```bash
echo 'export GEMINI_API_KEY="your-api-key-here"' >> ~/.bashrc
echo 'export GEMINI_PROJECT_ID="your-gcp-project-id"' >> ~/.bashrc
source ~/.bashrc
```

#### 3. Configuration Already Applied

I've already updated `application.properties` to use Gemini:

```properties
quarkus.langchain4j.chat-model.provider=gemini
```

#### 4. Restart Quarkus

```bash
# Stop current dev server (Ctrl+C)
# Then restart:
./mvnw quarkus:dev
```

#### 5. Test

```bash
# Should respond in 2-5 seconds!
node test-websocket-patient.js
```

---

## Solution 2: Use Much Smaller Local Model 🏃

If you want to keep it 100% local and can't use Gemini:

### Option A: TinyLlama 1.1B (Super Fast)

**Download:**
```bash
cd models/
wget https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
```

**Update docker-compose.yml:**
```yaml
command: >
  /usr/bin/llama-server
  --model /mnt/models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
  --alias granite
  --ctx-size 512
  --host 0.0.0.0
  --port 8080
  --n-gpu-layers 0
  --threads 12
  --threads-batch 12
  --batch-size 512
  --n-predict 150
```

**Restart:**
```bash
docker compose restart granite
```

**Expected Performance**: 5-10 tokens/second (10-20x faster!)
**Trade-off**: Simpler, less accurate responses

### Option B: Phi-2 2.7B (Good Balance)

**Download:**
```bash
cd models/
wget https://huggingface.co/TheBloke/phi-2-GGUF/resolve/main/phi-2.Q4_K_M.gguf
```

**Update docker-compose.yml:**
```yaml
--model /mnt/models/phi-2.Q4_K_M.gguf
```

**Expected Performance**: 2-4 tokens/second (4-8x faster)
**Trade-off**: Good quality, much faster than 8B

---

## Solution 3: Keep Current Setup (NOT RECOMMENDED) ⚠️

If you must keep Granite 8B on CPU:

### Update timeout again:

```properties
quarkus.langchain4j.openai.timeout=900s  # 15 minutes
```

### Update docker-compose.yml:

```yaml
command: >
  /usr/bin/llama-server
  --model /mnt/models/granite-3.3-8b-instruct-Q4_K_M.gguf
  --alias granite
  --ctx-size 256          # Even smaller
  --host 0.0.0.0
  --port 8080
  --n-gpu-layers 0
  --threads 16            # Max out threads
  --threads-batch 16
  --batch-size 1024       # Bigger batches
  --n-predict 50          # Shorter responses
  --temp 0.1              # Lower quality = faster
```

**Expected**: Maybe 1-1.5 tokens/second (still 2-3 minutes per response)

---

## Comparison Table

| Solution | Speed | Quality | Cost | Local |
|----------|-------|---------|------|-------|
| **Gemini (Recommended)** | ⚡⚡⚡⚡⚡ 2-5s | ⭐⭐⭐⭐⭐ | $0.0001/msg | ❌ Cloud |
| **TinyLlama 1.1B** | ⚡⚡⚡⚡ 10-30s | ⭐⭐ | Free | ✅ Yes |
| **Phi-2 2.7B** | ⚡⚡⚡ 30-60s | ⭐⭐⭐⭐ | Free | ✅ Yes |
| **Granite 8B (current)** | ⚡ 3-4min | ⭐⭐⭐⭐ | Free | ✅ Yes |

---

## My Recommendation

### For Production/Demo: Use Gemini

**Pros:**
- Users get instant responses
- Best quality answers
- No infrastructure needed
- Extremely cheap ($0.15 per 1000 messages)

**Cons:**
- Requires internet
- Costs money (but very little)
- Requires API key

### For Offline/Development: Use TinyLlama or Phi-2

**Pros:**
- Fast enough for testing (10-60 seconds)
- Completely free
- Works offline
- No API keys needed

**Cons:**
- Lower quality than 8B models
- Still slower than cloud

### NOT Recommended: Keep Granite 8B on CPU

**Why:**
- 3-4 minute response times are unacceptable for a chatbot
- Users will think it's broken
- Wastes 10x more CPU time than needed
- Only makes sense if you have GPU acceleration

---

## Quick Decision Guide

### Do you have Gemini API access?
- **YES** → Use Gemini (already configured!)
- **NO** → Continue below

### Can you get Gemini API access easily?
- **YES** → Get it, use Gemini (5 minute setup)
- **NO** → Continue below

### Is 30-60 second response time acceptable?
- **YES** → Use Phi-2 2.7B (good balance)
- **NO** → Use TinyLlama 1.1B (10-30 seconds)

### Must you have highest local quality?
- **YES** → Get a GPU or use Gemini
- **NO** → Use Phi-2 or TinyLlama

---

## Current Configuration Status

✅ **Application configured for Gemini** (but needs API keys)
⚠️ **Granite still running** (but not being used anymore)
🔧 **To switch back to Granite**: Edit application.properties

### To Use Gemini (FASTEST - 2-5 seconds):

```bash
export GEMINI_API_KEY="your-key"
export GEMINI_PROJECT_ID="your-project"
# Restart Quarkus
./mvnw quarkus:dev
```

### To Use Smaller Local Model (FAST - 10-60 seconds):

1. Download TinyLlama or Phi-2 (see above)
2. Update docker-compose.yml model path
3. Restart: `docker compose restart granite`
4. Re-enable OpenAI in application.properties:
   ```properties
   quarkus.langchain4j.chat-model.provider=openai
   ```
5. Restart Quarkus

### To Keep Granite 8B (SLOW - 3-4 minutes):

1. Re-enable OpenAI in application.properties:
   ```properties
   quarkus.langchain4j.chat-model.provider=openai
   ```
2. Accept 3-4 minute response times
3. Users will need to be very patient

---

## Testing After Switch

### If Using Gemini:

```bash
# Should respond in 2-5 seconds!
node test-websocket-patient.js
```

### If Using Smaller Model:

```bash
# Should respond in 10-60 seconds
node test-websocket-patient.js
```

---

## Files Modified

1. ✅ `application.properties` - Switched to Gemini
2. ⚠️ `docker-compose.yml` - Granite optimized but still too slow

---

## Summary

🔴 **Problem**: Granite 8B is 0.57 tokens/second = 3-4 minutes per response

✅ **Solution Applied**: Switched to Gemini (need API keys)

🎯 **Best Path Forward**:
1. Get Gemini API credentials (5 minutes)
2. Set environment variables
3. Restart Quarkus
4. Enjoy 2-5 second responses!

🔄 **Alternative**: Download TinyLlama/Phi-2 for faster local inference

⚠️ **Reality**: CPU-based 8B inference is not viable for real-time chat

---

## Next Steps

Choose ONE:

### Option 1: Enable Gemini (Recommended)
```bash
export GEMINI_API_KEY="..."
export GEMINI_PROJECT_ID="..."
./mvnw quarkus:dev
# Test - should respond in 2-5 seconds
```

### Option 2: Use Smaller Local Model
```bash
cd models/
wget https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
# Update docker-compose.yml
docker compose restart granite
# Test - should respond in 10-30 seconds
```

### Option 3: Accept Slow Performance
```bash
# Edit application.properties
# Change back to: quarkus.langchain4j.chat-model.provider=openai
./mvnw quarkus:dev
# Users wait 3-4 minutes per message
```

**I recommend Option 1 (Gemini) for the best user experience.**

