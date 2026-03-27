import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { ValidationService } from '../../core/services/validation.service';
import { AuthService } from '../../core/services/auth.service';
import { MatDialog } from '@angular/material/dialog';
import { NotificationDialog } from '../../core/components/pop-up/notification-component';
import { Router } from '@angular/router';
import { MatInput } from '../../core/components/input/input.component';
import { UserDto } from '../../core/dtos/user.dto';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  standalone: true,
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
  imports: [ReactiveFormsModule, CommonModule, MatInput, MatButton],
})
export class RegisterComponent {
  registerForm: FormGroup;
  errorMessage = '';
  private destroyRef = inject(DestroyRef);
  constructor(
    private fb: FormBuilder,
    private validationService: ValidationService,
    private authService: AuthService,
    private dialog: MatDialog,
    private router: Router,
  ) {
    this.registerForm = fb.group(
      {
        firstName: [null, validationService.firstNameValidator],
        lastName: [null, validationService.lastNameValidator],
        telephone: [null],
        email: [null, validationService.emailValidator],
        password: [null, validationService.strongPasswordValidator],
        confirmPassword: [null],
      },
      { validators: validationService.passwordMatchValidator },
    );
  }
  onRegister = () => {
    if (this.registerForm.valid) {
      console.log('Form is valid, submitting registration data:', this.registerForm.value);
      this.authService
        .createUser(this.registerForm.value as UserDto)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (user) => {
            this.dialog
              .open(NotificationDialog, {
                width: '400px',
                data: {
                  message: 'Registration successful! Please log in.',
                },
              })
              .afterClosed()
              .subscribe(() => {
                this.router.navigate(['/login']);
              });
          },
        });
    } else {
      this.errorMessage = 'Please fix the errors in the form before submitting.';
    }
  };
}
