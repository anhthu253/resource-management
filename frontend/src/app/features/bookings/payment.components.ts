import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { RadioButton } from '../../core/components/radio-button/radio-button.component';
import { MatInput } from '../../core/components/input/input.component';
import { ValidationService } from '../../core/services/validation.service';

@Component({
  standalone: true,
  selector: 'app-payment',
  templateUrl: './payment.component.html',
  imports: [ReactiveFormsModule, CommonModule, RadioButton, MatInput],
})
export class PaymentComponent implements OnInit {
  paymentMethods = ['Visa', 'Mastercard', 'Paypal'];
  selectedMethod = new FormControl<string>('');
  cardDetails: FormGroup;
  invalidCardMessage = '';
  constructor(private fb: FormBuilder, private validationService: ValidationService) {
    this.cardDetails = fb.group({
      cardNumber: ['', [this.validationService.visaCardValidator]],
      expiredDate: [null, Validators.required],
      cvv: ['', Validators.required],
      accountHolder: ['', Validators.required],
    });
  }
  ngOnInit() {}
}
