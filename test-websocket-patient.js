#!/usr/bin/env node

const WebSocket = require('ws');

console.log('🤖 AI Chatbot Test - Patient Version');
console.log('URL: ws://localhost:8082/caton/chatbot');
console.log('Note: LLM responses take 2-3 minutes on CPU - please be patient!\n');

const ws = new WebSocket('ws://localhost:8082/caton/chatbot', {
  timeout: 10000
});

let connected = false;
let greetingReceived = false;
let messagesSent = 0;
let responsesReceived = 0;
let startTime;

ws.on('open', function open() {
  connected = true;
  console.log('✅ WebSocket CONNECTED successfully!');
  console.log('⏳ Waiting for initial greeting...\n');
});

ws.on('message', function message(data) {
  const timestamp = new Date().toLocaleTimeString();
  console.log(`📥 [${timestamp}] RECEIVED from bot:`);
  console.log(`   Raw: ${data.toString()}`);

  try {
    const parsed = JSON.parse(data.toString());
    console.log(`   Message: "${parsed.message}"\n`);

    if (!greetingReceived) {
      greetingReceived = true;
      console.log('✅ Greeting received!\n');
      console.log('📤 Sending test question: "What is Infinispan?"\n');
      console.log('⏰ Please wait 2-3 minutes for LLM response...');
      console.log('   (This is normal for CPU-based inference)\n');

      startTime = Date.now();
      ws.send('What is Infinispan?');
      messagesSent++;
    } else {
      // This is the LLM response
      responsesReceived++;
      const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);
      console.log(`✅ LLM Response received in ${elapsed} seconds!`);
      console.log(`   Full response: "${parsed.message.substring(0, 200)}${parsed.message.length > 200 ? '...' : ''}"\n`);

      console.log('🎉 Test completed successfully!');
      console.log(`   Messages sent: ${messagesSent}`);
      console.log(`   Responses received: ${responsesReceived}`);
      console.log(`   Average response time: ${elapsed}s\n`);

      // Close after receiving response
      setTimeout(() => ws.close(), 1000);
    }
  } catch (e) {
    console.log('   (Could not parse as JSON)');
  }
});

ws.on('error', function error(err) {
  console.error('❌ WebSocket ERROR:');
  console.error('   Message:', err.message);
  if (err.code === 'ECONNREFUSED') {
    console.error('\n   Possible causes:');
    console.error('   - Quarkus not running on port 8082');
    console.error('   - Run: ./mvnw quarkus:dev');
  }
  process.exit(1);
});

ws.on('close', function close(code, reason) {
  if (connected && responsesReceived > 0) {
    console.log('🔌 Connection closed normally');
    console.log(`   Code: ${code}`);
    console.log(`   Reason: ${reason || '(none)'}`);
    process.exit(0);
  } else if (connected) {
    console.error('\n⚠️  Connection closed before receiving LLM response');
    console.error('   Possible causes:');
    console.error('   - Response took longer than expected');
    console.error('   - LLM server (Granite) not running');
    console.error('   - Timeout exceeded');
    console.error('\n   Check logs: docker logs granite-cpu-server');
    process.exit(1);
  } else {
    console.error('\n❌ Connection FAILED to establish');
    console.error('   Check: curl http://localhost:8082/caton/hello');
    process.exit(1);
  }
});

// Extend timeout to 5 minutes to match server-side
setTimeout(() => {
  if (responsesReceived === 0 && greetingReceived) {
    console.log('\n⏰ Still waiting for LLM response...');
    console.log('   This is taking longer than expected.');
    console.log('   Granite may be under heavy load or processing a complex response.');
    console.log('   Continuing to wait (will timeout after 5 minutes total)...\n');
  }
}, 120000); // 2 minute reminder

// Final timeout after 5 minutes
setTimeout(() => {
  if (responsesReceived === 0 && greetingReceived) {
    console.error('\n❌ TIMEOUT: No response after 5 minutes');
    console.error('   Check Granite logs: docker logs granite-cpu-server');
    console.error('   Check Quarkus logs for errors');
    ws.close();
    process.exit(1);
  }
}, 300000); // 5 minute timeout

// Keep alive messages
setInterval(() => {
  if (connected && greetingReceived && responsesReceived === 0) {
    console.log('   ⏳ Still waiting...');
  }
}, 30000); // Every 30 seconds

