import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatInput } from '../../core/components/input/input.component';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
@Component({
  standalone: true,
  selector: 'app-login',
  imports: [RouterLink, ReactiveFormsModule, CommonModule, MatInput],
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
  private destroyRef = inject(DestroyRef);
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
      this.loginService
        .login(data)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (res) => {
            this.userService.setUser(res);
            this.errorMessage = '';
            this.router.navigate(['/resources']);
          },
          error: (err) => {
            this.errorMessage = err.message || 'Login failed';
          },
        });
    }
  };
}
