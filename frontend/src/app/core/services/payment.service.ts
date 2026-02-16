import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { BookingStateService } from './booking.state.service';

@Injectable({ providedIn: 'root' })
export class PaymentGuard implements CanActivate {
  constructor(
    private bookingStateService: BookingStateService,
    private router: Router,
  ) {}
  canActivate(): boolean {
    const bookingId = this.bookingStateService.get()?.bookingId;
    if (!bookingId) {
      this.router.navigate(['/new-booking']);
      return false;
    }
    return true;
  }
}
