import { FormsModule } from '@angular/forms';
import { Component } from '@angular/core';
import { LoginService } from './login.service';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
@Component({
  standalone: true,
  selector: 'app-login',
  imports: [FormsModule, CommonModule],
  templateUrl: './login.component.html',
  providers: [LoginService],
})
export class LoginComponent {
  email = '';
  password = '';
  token: string | null = null;
  errorMessage: string | null = null;

  constructor(private loginService: LoginService, private router: Router) {}
  onLogin = () => {
    if (this.email && this.password) {
      const data = { username: this.email, password: this.password };
      this.loginService.getToken(data).subscribe({
        next: (res) => {
          this.token = res;
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
