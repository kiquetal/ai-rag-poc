import { Routes } from '@angular/router';
import { ChatbotComponent } from './chatbot.component';
import { HomeComponent } from './home.component';

export const routes: Routes = [
    { path: '', component: HomeComponent },
    { path: 'chatbot', component: ChatbotComponent }
];
