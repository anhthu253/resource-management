import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
} from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RadioButton } from '../../core/components/radio-button/radio-button.component';
import {
  loadStripe,
  Stripe,
  StripeCardCvcElement,
  StripeCardExpiryElement,
  StripeCardNumberElement,
} from '@stripe/stripe-js';
import { environment } from '../../../environments/environment';
import { PaymentIntentDto } from '../../core/dtos/booking.dto';
import { ActivatedRoute, Router } from '@angular/router';
import { BookingService } from '../../core/services/booking.service';
import { Popup } from '../../core/components/pop-up/pop-up.component';
import { MatButton } from '@angular/material/button';

@Component({
  standalone: true,
  selector: 'app-payment',
  templateUrl: './payment.component.html',
  styleUrl: './payment.component.css',
  imports: [ReactiveFormsModule, CommonModule, RadioButton, Popup, MatButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentComponent implements OnInit, AfterViewInit {
  paymentBrands = ['Visa', 'Mastercard', 'Paypal'];
  selectedBrand = new FormControl<string>('');
  detectedBrand = 'unknown';
  cardWarningMessage = '';
  paymentIntent: PaymentIntentDto = {
    bookingId: -1,
    paymentId: -1,
    paymentMethodId: '',
  };

  cardNumberError? = '';
  cardExpiryError? = '';
  isPaymentFailed: boolean = false;
  failPaymentmessage? = '';

  private stripe!: Stripe;
  private cardNumber!: StripeCardNumberElement;
  private cardExpiry!: StripeCardExpiryElement;
  private cardCvc!: StripeCardCvcElement;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private bookingService: BookingService,
    private changeDetector: ChangeDetectorRef,
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

    this.cardNumber.on('change', (event) => {
      this.cardNumberError = event.error?.message;
      this.detectedBrand = event.brand;
      this.checkBrandMismatch();
      this.changeDetector.markForCheck();
    });

    this.selectedBrand.valueChanges.subscribe(() => this.checkBrandMismatch());

    this.cardExpiry.on('change', (event) => {
      this.cardExpiryError = event.error?.message;
      this.changeDetector.markForCheck();
    });
  }
  private checkBrandMismatch() {
    const selectedBrand = this.selectedBrand.value?.toLowerCase();
    if (
      selectedBrand &&
      this.detectedBrand &&
      this.detectedBrand !== 'unknown' &&
      this.detectedBrand !== selectedBrand
    ) {
      this.cardWarningMessage = `You selected ${selectedBrand} but entered a ${this.detectedBrand} card.`;
    } else {
      this.cardWarningMessage = '';
    }
  }
  ngOnInit() {}
  async pay() {
    const { paymentMethod, error } = await this.stripe.createPaymentMethod({
      type: 'card',
      card: this.cardNumber,
    });

    if (error) {
      return;
    }
    this.paymentIntent.bookingId = this.route.snapshot.params['bookingId'];
    this.paymentIntent.paymentId = this.route.snapshot.params['paymentId'];
    this.paymentIntent.paymentMethodId = paymentMethod?.id;

    this.bookingService.createPaymentIntent(this.paymentIntent).subscribe({
      next: async (res) => {
        const result = await this.stripe.confirmCardPayment(res);
        if (result.paymentIntent?.status === 'succeeded') {
          this.router.navigate(
            ['../booking-summary', { bookingId: this.paymentIntent.bookingId }],
            {
              relativeTo: this.route,
            },
          );
        } else this.isPaymentFailed = true;
      },
      error: (err) => {
        this.failPaymentmessage = err;
      },
    });
  }
}
