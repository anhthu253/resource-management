import { Injectable } from '@angular/core';
import { AbstractControl, FormGroup, ValidationErrors } from '@angular/forms';

@Injectable({
  providedIn: 'root',
})
export class ValidationService {
  visaCardValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value.replace(/\s+/g, '');

    if (!value) return { required: 'This field is required' };

    // Check length: must be 16 digits
    if (!/^\d{16}$/.test(value)) {
      return { invalidLength: 'Invalid length' };
    }

    // Visa starts with 4
    if (!value.startsWith('4')) {
      return { notVisa: 'Invalid card number' };
    }

    // Luhn check
    if (!this.luhnCheck(value)) {
      return { invalidLuhn: true };
    }

    return null; // valid
  }

  luhnCheck(cardNumber: string): boolean {
    let sum = 0;
    let shouldDouble = false;

    for (let i = cardNumber.length - 1; i >= 0; i--) {
      let digit = parseInt(cardNumber[i], 10);

      if (shouldDouble) {
        digit *= 2;
        if (digit > 9) digit -= 9;
      }

      sum += digit;
      shouldDouble = !shouldDouble;
    }

    return sum % 10 === 0;
  }

  formatCardNumber(cardNumber: string) {
    if (cardNumber.length !== 16) return cardNumber;
    cardNumber = cardNumber.replace(/\D/g, '');

    // Limit to 16 digits (Visa length)
    cardNumber = cardNumber.substring(0, 16);

    // Insert a space every 4 digits
    return cardNumber.replace(/(.{4})/g, '$1 ').trim();
  }

  dateRangeValidator(group: FormGroup): ValidationErrors | null {
    const startedCtrl = group.get('startedAt');
    const endedCtrl = group.get('endedAt');

    if (!startedCtrl || !endedCtrl) return null;

    const startedAt = startedCtrl.value;
    const endedAt = endedCtrl.value;

    startedCtrl.setErrors(null);
    endedCtrl.setErrors(null);
    group.setErrors(null);

    if (!startedAt) {
      startedCtrl.setErrors({ required: 'This field is required' });
      return { required: true };
    }

    if (!endedAt) {
      endedCtrl.setErrors({ required: 'This field is required' });
      return { required: true };
    }

    if (startedAt <= new Date()) {
      startedCtrl.setErrors({ startedInThePast: 'Start date can not be the past' });
      return { invalidDate: true };
    }

    if (endedAt <= new Date()) {
      endedCtrl.setErrors({ endedInThePast: 'End date can not be the past' });
      return { invalidDate: true };
    }

    if (endedCtrl.dirty && startedAt >= endedAt) {
      startedCtrl.setErrors({ invalidStart: 'Start date has to be before end date' });
      endedCtrl.setErrors({ invalidEnd: 'End date has to be after start date' });
      return { invalidDateRange: true };
    }

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
