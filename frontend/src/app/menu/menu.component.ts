import { Component } from '@angular/core';
import { MatTabChangeEvent, MatTabsModule } from '@angular/material/tabs';
import { Router, RouterOutlet } from '@angular/router';
import { NewBookingComponent } from '../features/bookings/create-new-booking.component';
@Component({
  standalone: true,
  selector: 'app-menu',
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.css',
  imports: [RouterOutlet, MatTabsModule],
})
export class MenuComponent {
  constructor(private router: Router) {}
  onTabChange = (event: MatTabChangeEvent) => {
    switch (event.index) {
      case 0:
        this.router.navigate(['/new-booking']);
        break;
      case 1:
        this.router.navigate(['/my-bookings']);
        break;
      case 2:
        this.router.navigate(['/resources']);
        break;
    }
  };
  /*   toDashboard = () => {
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
  */
}
