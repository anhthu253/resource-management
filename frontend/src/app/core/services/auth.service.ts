import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UserDto } from '../dtos/user.dto';
import { environment } from '../../../environments/environment.prod';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private http: HttpClient) {}
  login(data: { username: string; password: string }): Observable<UserDto> {
    return this.http.post<UserDto>(`${environment.apiUrl}/auth/authenticate`, data, {
      withCredentials: true,
    });
  }
  logout(): Observable<void> {
    return this.http.post<void>(
      `${environment.apiUrl}/auth/logout`,
      {},
      {
        withCredentials: true,
      },
    );
  }
  getCurrentUser(): Observable<UserDto> {
    return this.http.get<UserDto>(`${environment.apiUrl}/auth/current-user`, {
      withCredentials: true,
    });
  }
}
