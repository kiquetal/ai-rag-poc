import { Component, OnInit, OnDestroy } from '@angular/core';
import { WebSocketService } from './websocket.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

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
  private socket$;

  constructor(private webSocketService: WebSocketService) {}

  ngOnInit(): void {
    const wsUrl = 'ws://' + window.location.host + '/chatbot';
    this.socket$ = this.webSocketService.connect(wsUrl);
    this.socket$.subscribe(
      (message) => {
        this.messages.push({ sender: 'Bot', content: message });
      },
      (err) => console.error(err),
      () => console.warn('Completed!')
    );
  }

  sendMessage(): void {
    if (this.newMessage.trim() !== '') {
      this.messages.push({ sender: 'You', content: this.newMessage });
      this.webSocketService.sendMessage(this.newMessage);
      this.newMessage = '';
    }
  }

  ngOnDestroy(): void {
    this.webSocketService.close();
  }
}

