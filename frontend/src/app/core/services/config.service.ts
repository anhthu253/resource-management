import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ConfigService {
  private config: any;

  load(): Promise<void> {
    return fetch('/assets/config.json')
      .then((r) => r.json())
      .then((c) => (this.config = c));
  }

  get apiUrl(): string {
    return this.config.apiUrl;
  }
}
