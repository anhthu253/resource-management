import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, OnInit } from '@angular/core';
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
import { StripeService } from '../../core/services/stripe.service';
import {
  loadStripe,
  Stripe,
  StripeCardCvcElement,
  StripeCardExpiryElement,
  StripeCardNumberElement,
} from '@stripe/stripe-js';
import { environment } from '../../../environments/environment';
import {
  PaymentIntentDto,
  BookingRequestDto,
  BookingResponseDto,
} from '../../core/dtos/booking.dto';
import { ActivatedRoute, Route, Router } from '@angular/router';
import { BookingService } from '../../core/services/booking.service';

@Component({
  standalone: true,
  selector: 'app-payment',
  templateUrl: './payment.component.html',
  imports: [ReactiveFormsModule, CommonModule, RadioButton, MatInput],
})
export class PaymentComponent implements OnInit, AfterViewInit {
  paymentMethods = ['Visa', 'Mastercard', 'Paypal'];
  selectedMethod = new FormControl<string>('');
  error? = '';
  paymentIntent: PaymentIntentDto = {
    bookingId: -1,
    paymentId: -1,
    paymentMethodId: '',
  };

  private stripe!: Stripe;
  private cardNumber!: StripeCardNumberElement;
  private cardExpiry!: StripeCardExpiryElement;
  private cardCvc!: StripeCardCvcElement;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private bookingService: BookingService
  ) {}

  async ngAfterViewInit() {
    this.stripe = (await loadStripe(environment.stripePublishableKey)) as Stripe;

    const elements = this.stripe.elements();

    // Create individual elements
    this.cardNumber = elements.create('cardNumber');
    this.cardExpiry = elements.create('cardExpiry');
    this.cardCvc = elements.create('cardCvc');

    // Mount them into separate divs
    this.cardNumber.mount('#card-number');
    this.cardExpiry.mount('#card-expiry');
    this.cardCvc.mount('#card-cvc');
  }
  ngOnInit() {}
  async pay() {
    const { paymentMethod, error } = await this.stripe.createPaymentMethod({
      type: 'card',
      card: this.cardNumber,
    });

    if (error) {
      this.error = error.message;
      console.log('error', this.error);
      return;
    }
    this.paymentIntent.bookingId = this.route.snapshot.params['bookingId'];
    this.paymentIntent.paymentId = this.route.snapshot.params['paymentId'];
    this.paymentIntent.paymentMethodId = paymentMethod?.id;

    this.bookingService.createPaymentIntent(this.paymentIntent).subscribe({
      next: async (res) => {
        const result = await this.stripe.confirmCardPayment(res);
        if (result.paymentIntent?.status === 'succeeded')
          this.router.navigate(
            ['../booking-summary', { bookingId: this.paymentIntent.bookingId }],
            {
              relativeTo: this.route,
            }
          );
        else console.log('Your payment has not been proceeded. Please try again!');
      },
      error: (err) => {
        console.log('error creating payment intent', err);
      },
    });
  }
}
