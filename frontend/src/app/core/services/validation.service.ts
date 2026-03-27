import { Injectable } from '@angular/core';
import { AbstractControl, FormGroup, ValidationErrors } from '@angular/forms';

@Injectable({
  providedIn: 'root',
})
export class ValidationService {
  firstNameValidator = (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string;
    if (!value) return { required: 'Please enter your first name.' };
    return null;
  };

  lastNameValidator = (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string;
    if (!value) return { required: 'Please enter your last name.' };
    return null;
  };

  emailValidator = (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string;
    if (!value) return { required: 'Please enter your email address.' };

    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    const result = emailRegex.test(value) ? null : { email: 'Please enter a valid email address.' };
    return result;
  };

  strongPasswordValidator = (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) return { required: 'Please enter a password' };

    const hasUpper = /[A-Z]/.test(value);
    const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(value);
    const hasMinLen = value.length >= 8;

    const valid = hasUpper && hasSpecial && hasMinLen;
    return valid
      ? null
      : {
          strongPassword:
            'Password must contain an uppercase letter, a special character, and be at least 8 characters long',
        };
  };

  passwordMatchValidator(group: FormGroup): ValidationErrors | null {
    const passwordCtrl = group.get('password');
    const confirmCtrl = group.get('confirmPassword');

    if (!passwordCtrl || !confirmCtrl) return null;

    const password = passwordCtrl.value;
    const confirm = confirmCtrl.value;

    if (password && confirm && password !== confirm) {
      confirmCtrl.setErrors({ passwordMismatch: 'Passwords do not match' });
      return { passwordMismatch: true };
    } else {
      if (!confirm && confirmCtrl.dirty && password) {
        confirmCtrl.setErrors({ required: 'Please confirm your password' });
        return { required: true };
      }
      confirmCtrl.setErrors(null);
    }

    return null;
  }

  dateRangeValidator(group: FormGroup): ValidationErrors | null {
    const startedCtrl = group.get('startedAt');
    const endedCtrl = group.get('endedAt');

    if (!startedCtrl || !endedCtrl) return null;

    const startedAt = startedCtrl.value;
    const endedAt = endedCtrl.value;

    if (startedCtrl.dirty && !startedAt) {
      startedCtrl.setErrors({ required: 'Please select a start date' });
      return { required: true };
    }

    if (!endedAt) {
      endedCtrl.setErrors({ required: 'Please select an end date' });
      return { required: true };
    }

    if (startedAt && startedAt <= new Date()) {
      startedCtrl.setErrors({ startedInThePast: 'Start date can not be the past' });
      return { invalidDate: true };
    }

    if (endedAt && endedAt <= new Date()) {
      endedCtrl.setErrors({ endedInThePast: 'End date can not be the past' });
      return { invalidDate: true };
    }

    if (startedAt && endedAt && startedAt >= endedAt) {
      startedCtrl.setErrors({ invalidStart: 'Start has to be before end' });
      endedCtrl.setErrors({ invalidEnd: 'End has to be after start' });
      return { invalidDateRange: true };
    }
    startedCtrl.setErrors(null);
    endedCtrl.setErrors(null);
    group.setErrors(null);
    return null;
  }
  bookingFormValidation = (formGroup: FormGroup): ValidationErrors | null => {
    const periodGroup = formGroup.get('period');
    if (!periodGroup) return null;
    if (periodGroup.invalid) return null;

    const resourceCtrl = formGroup.get('resourceIds');
    if (!resourceCtrl) return null;

    resourceCtrl.setErrors(null);
    if (!resourceCtrl.value || !resourceCtrl.value.length) {
      resourceCtrl.setErrors({ resourceRequired: 'Please select at least one resource' });
      return { required: true };
    }
    return null;
  };
}
