import { Injectable } from '@angular/core';
import {
  loadStripe,
  StripeCardCvcElement,
  StripeCardExpiryElement,
  StripeCardNumberElement,
} from '@stripe/stripe-js';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class StripeService {
  private stripePromise = loadStripe(environment.stripePublishableKey);
  private cardNumber!: StripeCardNumberElement;
  private cardExpiry!: StripeCardExpiryElement;
  private cardCvc!: StripeCardCvcElement;

  async createCardElements() {
    const stripe = await this.stripePromise;
    if (!stripe) throw new Error('Stripe failed to load');

    const elements = stripe.elements();
    this.cardNumber = elements.create('cardNumber');
    this.cardExpiry = elements.create('cardExpiry');
    this.cardCvc = elements.create('cardCvc');
    this.cardNumber.mount('#card-number');
    this.cardExpiry.mount('#card-expiry');
    this.cardCvc.mount('#card-cvc');
  }

  async createToken(): Promise<{ token: string | undefined; error: string | undefined }> {
    const stripe = await this.stripePromise;
    if (!stripe) throw new Error('Stripe failed to load');

    const { token, error } = await stripe.createToken(this.cardNumber);
    return { token: token?.id, error: error?.message };
  }
}
