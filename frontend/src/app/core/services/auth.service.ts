import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UserDto } from '../dtos/user.dto';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private http: HttpClient) {}
  login(data: { username: string; password: string }): Observable<UserDto> {
    return this.http.post<UserDto>('http://localhost:8080/auth/authenticate', data, {
      withCredentials: true,
    });
  }
  getCurrentUser(): Observable<UserDto> {
    return this.http.get<UserDto>('http://localhost:8080/auth/current-user', {
      withCredentials: true,
    });
  }
}
