import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeartbeatService } from './core/services/heartbeat.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('my-angular-app');
  constructor(private heartbeat: HeartbeatService) {
    this.heartbeat.start();
  }
}
