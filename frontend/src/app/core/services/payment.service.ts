import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class PaymentGuard implements CanActivate {
  constructor(private router: Router) {}
  //users are only allowed to navigate form /new-booking to /payment. Prevent user from typing /payment directly
  canActivate(route: ActivatedRouteSnapshot): boolean {
    this.router.navigate(['/new-booking']);
    return false;
  }
}
