import { Component, OnInit, OnDestroy } from '@angular/core';
import { WebSocketService } from './websocket.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WebSocketSubject } from 'rxjs/webSocket';

@Component({
  selector: 'app-chatbot',
  template: `
    <div class="chat-container">
      <div class="messages">
        <div *ngFor="let message of messages" class="message">
          <strong>{{ message.sender }}:</strong> {{ message.content }}
        </div>
      </div>
      <div class="input-area">
        <input [(ngModel)]="newMessage" (keyup.enter)="sendMessage()" placeholder="Type a message...">
        <button (click)="sendMessage()">Send</button>
      </div>
    </div>
  `,
  styles: [`
    .chat-container {
      display: flex;
      flex-direction: column;
      height: 300px;
      border: 1px solid #ccc;
      padding: 10px;
    }
    .messages {
      flex-grow: 1;
      overflow-y: auto;
      margin-bottom: 10px;
    }
    .input-area {
      display: flex;
    }
    input {
      flex-grow: 1;
      padding: 5px;
    }
    button {
      margin-left: 5px;
    }
  `],
  imports: [CommonModule, FormsModule],
  standalone: true
})
export class ChatbotComponent implements OnInit, OnDestroy {
  messages: { sender: string, content: string }[] = [];
  newMessage: string = '';
  private socket$!: WebSocketSubject<any>;

  constructor(private webSocketService: WebSocketService) {}

  ngOnInit(): void {
    console.log('ChatbotComponent ngOnInit');
    const wsUrl = 'ws://' + window.location.hostname + ':8082/caton/chatbot';
    console.log('WebSocket URL:', wsUrl);
    this.socket$ = this.webSocketService.connect(wsUrl);
    console.log('Subscribing to WebSocket');
    this.socket$.subscribe(
      (message) => {
        console.log('Received message:', message);
        this.messages.push({ sender: 'Bot', content: message });
      },
      (err) => console.error('WebSocket error:', err),
      () => console.warn('WebSocket connection completed!')
    );
    console.log('Subscription added');
  }

  sendMessage(): void {
    console.log('sendMessage called');
    if (this.newMessage.trim() !== '') {
      this.messages.push({ sender: 'You', content: this.newMessage });
      console.log('Sending message:', this.newMessage);
      this.webSocketService.sendMessage(this.newMessage);
      this.newMessage = '';
    }
  }

  ngOnDestroy(): void {
    this.webSocketService.close();
  }
}
