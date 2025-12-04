import { FormsModule } from '@angular/forms';
import { Component } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { UserService } from '../../core/services/user.service';
import { UserDto } from '../../core/dtos/user.dto';
@Component({
  standalone: true,
  selector: 'app-login',
  imports: [FormsModule, CommonModule],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  email = '';
  password = '';
  token: string | null = null;
  errorMessage: string | null = null;

  constructor(
    private loginService: AuthService,
    private userService: UserService,
    private router: Router
  ) {}
  onLogin = () => {
    if (this.email && this.password) {
      const data = { username: this.email, password: this.password };
      this.loginService.login(data).subscribe({
        next: (res) => {
          this.userService.setUser(res);
          this.errorMessage = '';
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.errorMessage = err.message || 'Login failed';
        },
      });
    }
  };
}
