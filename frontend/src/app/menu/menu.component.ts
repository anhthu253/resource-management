import { Component } from '@angular/core';
import { Router } from '@angular/router';
@Component({
  standalone: true,
  selector: 'app-menu',
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.css',
})
export class MenuComponent {
  constructor(private router: Router) {}
  toDashboard = () => {
    this.router.navigate(['/dashboard']);
  };
  createBooking = () => {
    this.router.navigate(['/new-booking']);
  };
  toMyBookings = () => {
    this.router.navigate(['/my-booking']);
  };
  toResources = () => {
    this.router.navigate(['/resources']);
  };
  toCalendar = () => {
    this.router.navigate(['/calendar']);
  };
  toProfile = () => {
    this.router.navigate(['/profile']);
  };
}
