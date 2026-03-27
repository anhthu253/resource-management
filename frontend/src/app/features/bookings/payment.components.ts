import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  inject,
  OnDestroy,
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
import { BookingDto, PaymentIntentDto } from '../../core/dtos/booking.dto';
import { ActivatedRoute, NavigationStart, Router } from '@angular/router';
import { BookingService } from '../../core/services/booking.service';
import { MatButton } from '@angular/material/button';
import { NotificationDialog } from '../../core/components/pop-up/notification-component';
import { MatDialog } from '@angular/material/dialog';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BookingStateService } from '../../core/services/booking.state.service';
import { Subscription } from 'rxjs';

@Component({
  standalone: true,
  selector: 'app-payment',
  templateUrl: './payment.component.html',
  styleUrl: './payment.component.css',
  imports: [ReactiveFormsModule, CommonModule, RadioButton, MatButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentComponent implements AfterViewInit, OnDestroy, OnInit {
  private navSub!: Subscription;

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

  private destroyRef = inject(DestroyRef);

  constructor(
    private router: Router,
    private bookingService: BookingService,
    private bookingStateService: BookingStateService,
    private changeDetector: ChangeDetectorRef,
    private dialog: MatDialog,
  ) {}

  get currentBooking(): BookingDto | null {
    return this.bookingStateService.getBooking();
  }

  ngOnDestroy(): void {
    this.navSub.unsubscribe();
  }

  ngOnInit(): void {
    //subscribe to navigation events
    this.navSub = this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        // check if user is leaving payment
        const goingBackToBooking = event.url.includes('/new-booking');
        if (!goingBackToBooking) {
          // clear booking if NOT going back to booking
          this.bookingStateService.clearBooking();
        }
      }
    });
  }

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

  goBack() {
    this.router.navigate(['/new-booking'], {
      state: { fromPayment: true },
    });
  }
  async pay() {
    const { paymentMethod, error } = await this.stripe.createPaymentMethod({
      type: 'card',
      card: this.cardNumber,
    });

    if (error) {
      return;
    }

    const booking = this.bookingStateService.getBooking();
    if (!booking || !booking.bookingGroupId || !booking.paymentId) return;
    this.paymentIntent.bookingId = booking.bookingId;
    this.paymentIntent.paymentId = booking.paymentId;
    this.paymentIntent.paymentMethodId = paymentMethod?.id;
    this.bookingService
      .createPaymentIntent(this.paymentIntent)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: async (res) => {
          const result = await this.stripe.confirmCardPayment(res);
          if (result.paymentIntent?.status === 'succeeded') {
            if (booking.replacedBookingId) {
              //create refund for the previous booking
              this.bookingService.createRefund(booking.replacedBookingId).subscribe({
                next: (res) => {
                  this.router.navigate(['/booking-summary'], {
                    queryParams: {
                      bookingId: booking.bookingId,
                      refund: true,
                    },
                  });
                },
                error: (err) => {},
              }); //create a refund for the original payment of the current booking, which will be processed asynchronously by the backend. Users will receive email notifications from Stripe about the refund status.
            } else {
              this.router.navigate(['/booking-summary'], {
                queryParams: {
                  bookingId: booking.bookingId,
                  refund: false,
                },
              });
            }
          } else this.openDialog('Payment is not processed. Please try again later.');
        },
        error: (err) => {
          this.openDialog(
            err.error || err.message || 'Failed to process payment. Please try again later.',
          );
        },
      });
  }
}
