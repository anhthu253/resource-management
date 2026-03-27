import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router } from '@angular/router';
import { BookingStateService } from './booking.state.service';

@Injectable({ providedIn: 'root' })
export class PaymentGuard implements CanActivate {
  constructor(
    private bookingStateService: BookingStateService,
    private router: Router,
  ) {}
  //users are only allowed to navigate form /new-booking to /payment. Prevent user from typing /payment directly
  canActivate(route: ActivatedRouteSnapshot): boolean {
    const bookingId = this.bookingStateService.getBooking()?.bookingId;
    if (!bookingId) {
      this.router.navigate(['/new-booking']);
      return false;
    }

    return true;
  }
}
