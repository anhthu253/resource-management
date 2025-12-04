import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

@Injectable()
export class LoginService {
  constructor(private http: HttpClient) {}
  getToken(data: { username: string; password: string }): Observable<string> {
    return this.http.post<string>('http://localhost:8080/auth/authenticate', data, {
      responseType: 'text' as 'json',
    });
  }
}
