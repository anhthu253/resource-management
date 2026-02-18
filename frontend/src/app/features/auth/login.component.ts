import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Component } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { UserService } from '../../core/services/user.service';
import { UserDto } from '../../core/dtos/user.dto';
import { MatInput } from '../../core/components/input/input.component';
import { validate } from '@angular/forms/signals';
import { MatButton } from '@angular/material/button';
@Component({
  standalone: true,
  selector: 'app-login',
  imports: [ReactiveFormsModule, CommonModule, MatInput, MatButton],
  templateUrl: './login.component.html',
  styles: `
    .login-input-row {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
  `,
})
export class LoginComponent {
  token: string | null = null;
  errorMessage: string | null = null;
  loginForm: FormGroup;

  constructor(
    private loginService: AuthService,
    private userService: UserService,
    private router: Router,
    private fb: FormBuilder,
  ) {
    this.loginForm = fb.group(
      {
        email: [null],
        password: [null],
      },
      Validators.required,
    );
  }
  onLogin = () => {
    if (this.loginForm.valid) {
      const data = {
        username: this.loginForm.get('email')?.value,
        password: this.loginForm.get('password')?.value,
      };
      this.loginService.login(data).subscribe({
        next: (res) => {
          this.userService.setUser(res);
          this.errorMessage = '';
          this.router.navigate(['/new-booking']);
        },
        error: (err) => {
          this.errorMessage = err.message || 'Login failed';
        },
      });
    }
  };
}
