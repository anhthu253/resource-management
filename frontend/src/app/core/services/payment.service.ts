import { Injectable } from '@angular/core';
import {
  ActivatedRoute,
  ActivatedRouteSnapshot,
  CanActivate,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import { BookingStateService } from './booking.state.service';

@Injectable({ providedIn: 'root' })
export class PaymentGuard implements CanActivate {
  constructor(private router: Router) {}
  //users are only allowed to navigate form /new-booking to /payment. Prevent user from typing /payment directly
  canActivate(route: ActivatedRouteSnapshot): boolean {
    const bookingId = route.queryParamMap.get('bookingId');
    if (!bookingId) {
      this.router.navigate(['/new-booking']);
      return false;
    }

    return true;
  }
}
