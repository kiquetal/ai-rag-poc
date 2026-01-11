#!/usr/bin/env node

// Simple WebSocket test client
const WebSocket = require('ws');

const ws = new WebSocket('ws://localhost:8082/caton/chatbot');

ws.on('open', function open() {
  console.log('✅ Connected to WebSocket');
  console.log('Waiting for initial message...\n');
});

ws.on('message', function message(data) {
  console.log('📥 Received:', data.toString());

  // Send a test message after receiving the greeting
  if (!ws.testSent) {
    ws.testSent = true;
    console.log('\n📤 Sending test message: "Hello"\n');
    ws.send('Hello');
  }
});

ws.on('error', function error(err) {
  console.error('❌ WebSocket error:', err.message);
  process.exit(1);
});

ws.on('close', function close() {
  console.log('\n🔌 Connection closed');
  process.exit(0);
});

// Auto-close after 10 seconds
setTimeout(() => {
  console.log('\n⏱️  Test timeout - closing connection');
  ws.close();
}, 10000);

