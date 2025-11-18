import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  template: `
    <h1>Welcome to the Chatbot App!</h1>
    <a routerLink="/chatbot">Go to Chatbot</a>
  `,
  imports: [RouterLink],
  standalone: true
})
export class HomeComponent {}
