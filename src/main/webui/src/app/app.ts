import { Component, signal } from '@angular/core';
import { environment } from '../environments/environment';
import { ChatbotComponent } from './chatbot.component';

@Component({
  selector: 'app-root',
  imports: [ChatbotComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('frontend');

  constructor() {
    console.log('Environment:', environment);
    console.log('API Base URL:', environment.apiBaseUrl);
  }
}
