#!/usr/bin/env node

const WebSocket = require('ws');

console.log('Testing WebSocket connection...');
console.log('URL: ws://localhost:8082/caton/chatbot\n');

const ws = new WebSocket('ws://localhost:8082/caton/chatbot', {
  timeout: 5000
});

let connected = false;

ws.on('open', function open() {
  connected = true;
  console.log('✅ WebSocket CONNECTED successfully!');
  console.log('⏳ Waiting for initial message from bot...\n');
});

ws.on('message', function message(data) {
  console.log('📥 RECEIVED from bot:');
  console.log('   Raw:', data.toString());

  try {
    const parsed = JSON.parse(data.toString());
    console.log('   Parsed:', JSON.stringify(parsed, null, 2));
  } catch (e) {
    console.log('   (Not JSON)');
  }

  if (!ws.testSent) {
    ws.testSent = true;
    console.log('\n📤 SENDING test message: "What is Infinispan?"\n');
    ws.send('What is Infinispan?');

    // Close after 5 more seconds
    setTimeout(() => {
      console.log('\n✅ Test completed successfully!');
      ws.close();
    }, 5000);
  }
});

ws.on('error', function error(err) {
  console.error('❌ WebSocket ERROR:');
  console.error('   Code:', err.code);
  console.error('   Message:', err.message);
  console.error('   Stack:', err.stack);
  process.exit(1);
});

ws.on('close', function close(code, reason) {
  if (connected) {
    console.log('\n🔌 Connection closed normally');
    console.log('   Code:', code);
    console.log('   Reason:', reason || '(none)');
  } else {
    console.error('\n❌ Connection FAILED to establish');
    console.error('   Possible causes:');
    console.error('   - Quarkus not running on port 8082');
    console.error('   - WebSocket endpoint not available at /caton/chatbot');
    console.error('   - ChatbotService initialization failed');
    console.error('   - LLM server not responding');
    console.error('\n   Debug steps:');
    console.error('   1. Check Quarkus logs for errors');
    console.error('   2. Verify: curl http://localhost:8082/caton/hello');
    console.error('   3. Verify: curl http://localhost:8080/v1/models');
  }
  process.exit(connected ? 0 : 1);
});

// Timeout after 10 seconds
setTimeout(() => {
  if (!connected) {
    console.error('\n⏱️  CONNECTION TIMEOUT (10s)');
    console.error('The WebSocket never connected. Quarkus might not be running.');
    ws.close();
  }
}, 10000);

