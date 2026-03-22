import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { interval, switchMap } from 'rxjs';
import { Router } from '@angular/router';
import { UserService } from './user.service';

@Injectable({ providedIn: 'root' })
export class HeartbeatService {
  constructor(
    private http: HttpClient,
    private router: Router,
    private user: UserService,
  ) {}

  start() {
    interval(5 * 60 * 1000) // every 5 minutes
      .pipe(switchMap(() => this.http.get('/api/auth/ping')))
      .subscribe({
        error: () => {
          // session expired
          this.user.clearUser();
          this.router.navigate(['/login']);
        },
      });
  }
}
