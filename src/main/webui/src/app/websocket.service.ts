import { Injectable } from '@angular/core';
}
  }
    this.socket$.complete();
  public close(): void {

  }
    this.socket$.next(message);
  public sendMessage(message: any): void {

  }
    return this.socket$;
    }
      this.socket$ = webSocket(url);
    if (!this.socket$ || this.socket$.closed) {
  public connect(url: string): WebSocketSubject<any> {

  private socket$: WebSocketSubject<any>;
export class WebSocketService {
})
  providedIn: 'root'
@Injectable({

import { webSocket, WebSocketSubject } from 'rxjs/webSocket';

