import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UserDto } from '../dtos/user.dto';
import { ConfigService } from './config.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(
    private http: HttpClient,
    private configService: ConfigService,
  ) {}
  login(data: { username: string; password: string }): Observable<UserDto> {
    return this.http.post<UserDto>(`${this.configService.apiUrl}/auth/authenticate`, data, {
      withCredentials: true,
    });
  }
  logout(): Observable<void> {
    return this.http.post<void>(
      `${this.configService.apiUrl}/auth/logout`,
      {},
      {
        withCredentials: true,
      },
    );
  }
  createUser(user: UserDto): Observable<UserDto> {
    return this.http.post<UserDto>(`${this.configService.apiUrl}/auth/register`, user);
  }

  getCurrentUser(): Observable<UserDto> {
    return this.http.get<UserDto>(`${this.configService.apiUrl}/auth/current-user`, {
      withCredentials: true,
    });
  }
}
