import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { UserDto } from '../dtos/user.dto';

@Injectable({ providedIn: 'root' })
export class UserService {
  private userSubject = new BehaviorSubject<UserDto | null>(null);
  user$ = this.userSubject.asObservable();

  setUser(user: UserDto) {
    this.userSubject.next(user);
  }

  clearUser() {
    this.userSubject.next(null);
  }

  getUser() {
    return this.userSubject.value;
  }
}
