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
import { MatButton } from '@angular/material/button';
import { BookingStateService } from '../../core/services/booking.state.service';
import { NotificationDialog } from '../../core/components/pop-up/notification-component';
import { MatDialog } from '@angular/material/dialog';

@Component({
  standalone: true,
  selector: 'app-payment',
  templateUrl: './payment.component.html',
  styleUrl: './payment.component.css',
  imports: [ReactiveFormsModule, CommonModule, RadioButton, MatButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentComponent implements AfterViewInit {
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
    private dialog: MatDialog,
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
  openDialog = (message: string) => {
    const dialogRef = this.dialog.open(NotificationDialog, {
      width: '350px',
      data: {
        message: message,
      },
    });
  };

  async pay() {
    const { paymentMethod, error } = await this.stripe.createPaymentMethod({
      type: 'card',
      card: this.cardNumber,
    });

    if (error) {
      return;
    }

    this.route.queryParamMap.subscribe((params) => {
      const bookingId = params.get('bookingId');
      const paymentId = params.get('paymentId');
      if (!bookingId || !paymentId) return;
      this.paymentIntent.bookingId = +bookingId;
      this.paymentIntent.paymentId = +paymentId;
      this.paymentIntent.paymentMethodId = paymentMethod?.id;
      this.bookingService.createPaymentIntent(this.paymentIntent).subscribe({
        next: async (res) => {
          const result = await this.stripe.confirmCardPayment(res);
          if (result.paymentIntent?.status === 'succeeded') {
            this.router.navigate(['/booking-summary'], {
              queryParams: {
                bookingId: bookingId,
              },
            });
          } else this.openDialog('Payment is not processed. Please try again later.');
        },
        error: (err) => {
          this.openDialog(err.error);
        },
      });
    });
  }
}
