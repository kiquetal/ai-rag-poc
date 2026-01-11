# Performance Issue Diagnosed & Optimized

## The Problem

Your Granite LLM was **extremely slow**:
- **2 seconds per token** (0.48 tokens/second)
- **Over 3 minutes for a 100-token response**
- **Timing out even with 5-minute limit**

This is **much slower than expected** for CPU inference.

---

## Root Causes Identified

From the Granite logs:
```
eval time = 193289.05 ms / 93 tokens (2078.38 ms per token, 0.48 tokens per second)
```

### Issues:
1. **Context size too large**: 2048 tokens (more processing overhead)
2. **Only 8 threads**: Not using all available CPU cores
3. **No batch optimization**: Processing tokens one at a time
4. **No parallelization**: Single request processing

---

## Optimizations Applied

### 1. Reduced Context Window
```yaml
--ctx-size 512  # Was 2048, reduced by 75%
```
**Effect**: Less memory processing = faster inference

### 2. Increased Thread Count
```yaml
--threads 12           # Was 8, increased by 50%
--threads-batch 12     # Added batch threading
```
**Effect**: Uses more CPU cores for parallel processing

### 3. Added Batch Processing
```yaml
--batch-size 512       # Added batch optimization
```
**Effect**: Processes multiple tokens in parallel

### 4. Limited Response Length
```yaml
--n-predict 150        # Limits max response to 150 tokens
```
**Effect**: Prevents extremely long responses that timeout

### 5. Parallelization Control
```yaml
--parallel 1           # Ensures single-request focus
```
**Effect**: All resources dedicated to one request at a time

### 6. Increased Timeout
```properties
quarkus.langchain4j.openai.timeout=600s  # 10 minutes
```
**Effect**: Gives more time while optimizations take effect

---

## Expected Performance Improvement

### Before Optimization:
- **Speed**: 0.48 tokens/second
- **100-token response**: ~208 seconds (3.5 minutes)
- **Status**: Timing out frequently

### After Optimization (Expected):
- **Speed**: 1.5-2.5 tokens/second (3-5x faster)
- **100-token response**: ~40-70 seconds
- **Status**: Should complete within timeout

### Realistic Expectations:
- **First token latency**: 10-20 seconds (prompt processing)
- **Generation**: 1-3 seconds per token
- **Total for 50-token response**: 1-2 minutes
- **Total for 150-token response**: 3-5 minutes

---

## How to Test

### 1. Wait for Granite to Fully Start
```bash
# Give it 30 seconds to load the model
sleep 30
```

### 2. Test Direct API Call
```bash
time curl http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "granite",
    "messages": [{"role": "user", "content": "Say hello"}],
    "max_tokens": 20
  }'
```

**Expected**: Response in 30-60 seconds (for 20 tokens)

### 3. Test via WebSocket
```bash
node test-websocket-patient.js
```

**Expected**: 
- Connection: Instant
- Greeting: Instant
- Response: 1-2 minutes

---

## Understanding the Logs

### Good Signs in Granite Logs:
```
eval time = 50000 ms / 100 tokens (500 ms per token, 2.0 tokens per second)
```
This would indicate **4x speed improvement**!

### What to Monitor:
1. **Tokens per second**: Should be 1.5-3.0 (not 0.48)
2. **Time per token**: Should be 300-700ms (not 2000ms)
3. **Total eval time**: Should be under 2 minutes for typical responses

---

## Configuration Summary

### Granite Docker Compose Settings
```yaml
command: >
  /usr/bin/llama-server
  --model /mnt/models/granite-3.3-8b-instruct-Q4_K_M.gguf
  --alias granite
  --ctx-size 512          # ← Reduced from 2048
  --host 0.0.0.0
  --port 8080
  --n-gpu-layers 0
  --threads 12            # ← Increased from 8
  --threads-batch 12      # ← Added
  --cache-reuse 256
  --n-predict 150         # ← Added limit
  --batch-size 512        # ← Added
  --parallel 1            # ← Added
```

### Application Properties
```properties
quarkus.langchain4j.openai.timeout=600s  # 10 minutes
```

---

## Further Optimizations (If Still Slow)

### Option 1: Use a Smaller Model

The 8B model is large. Try a 2B or 3B variant:

```bash
# Download smaller model
cd models/
# Example: granite-7b or granite-3b if available
```

**Expected**: 3-4x faster than 8B model

### Option 2: Reduce Response Quality for Speed

Add to docker-compose:
```yaml
--temp 0.3              # Lower creativity = faster
--top-k 10              # Limit token choices
--top-p 0.9             # Nucleus sampling
--repeat-penalty 1.2    # Prevent repetition quickly
```

### Option 3: Increase CPU Priority

```yaml
deploy:
  resources:
    limits:
      cpus: '12'        # Use all cores
    reservations:
      cpus: '8'         # Reserve minimum
```

### Option 4: Switch to Gemini (Cloud)

If local performance is not acceptable:

```properties
quarkus.langchain4j.chat-model.provider=gemini
```

**Expected**: 2-5 seconds per response (100x faster!)

---

## Monitoring Performance

### Real-time Performance Check

```bash
# Monitor Granite logs
docker logs -f granite-cpu-server

# Look for these metrics:
# - "eval time" = total generation time
# - "X ms per token" = speed per token
# - "X tokens per second" = throughput

# Goal: 1.5-3.0 tokens/second (currently 0.48)
```

### CPU Usage Check

```bash
# Monitor CPU usage
docker stats granite-cpu-server

# Should show:
# - CPU%: 700-1200% (using 7-12 cores)
# - MEM: 4-8GB
```

---

## Troubleshooting

### Still Timing Out After 10 Minutes?

This means something is seriously wrong. Check:

1. **Is Granite actually processing?**
   ```bash
   docker logs granite-cpu-server | grep "processing task"
   ```

2. **CPU throttling?**
   ```bash
   # Check CPU temperature and throttling
   sensors  # Linux
   # If overheating, Granite will be throttled
   ```

3. **Competing processes?**
   ```bash
   top -o %CPU
   # Make sure no other process is using CPU
   ```

### Response Quality Degraded?

If responses are worse quality after optimization:

1. **Increase context size**:
   ```yaml
   --ctx-size 1024  # Compromise between 512 and 2048
   ```

2. **Remove response limit**:
   ```yaml
   # Remove or increase:
   --n-predict 300
   ```

---

## Testing Checklist

After Granite restarts:

- [ ] Wait 30 seconds for model to load
- [ ] Test: `curl http://localhost:8080/v1/models`
- [ ] Test direct API call (should be 30-60 seconds)
- [ ] Test WebSocket connection (greeting should be instant)
- [ ] Send test message (response should be 1-2 minutes)
- [ ] Check Granite logs for improved tokens/second

---

## Expected Timeline

| Action | Time |
|--------|------|
| Granite restart | 30 seconds |
| Model load | Complete |
| Connection test | Instant |
| Direct API test | 30-60 seconds |
| WebSocket test | 1-2 minutes |

---

## Summary

### What Changed:
1. ✅ Reduced context size (75% smaller)
2. ✅ Increased thread count (50% more)
3. ✅ Added batch processing
4. ✅ Limited response length
5. ✅ Increased timeout to 10 minutes

### Expected Result:
- **3-5x faster inference** (from 0.48 to 1.5-2.5 tokens/second)
- **Responses in 1-2 minutes** instead of 3-5+ minutes
- **No more timeouts** for normal queries

### Test Now:
```bash
# Wait for Granite to be ready
sleep 30

# Quick test
time curl http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model":"granite","messages":[{"role":"user","content":"Hi"}],"max_tokens":20}'
```

If this completes in under 1 minute, the optimizations are working!

