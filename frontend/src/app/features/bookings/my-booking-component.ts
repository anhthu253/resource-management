import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { BookingService } from '../../core/services/booking.service';
import { BookingDto } from '../../core/dtos/booking.dto';
import { CommonModule } from '@angular/common';
import { UserService } from '../../core/services/user.service';
import { MatCard, MatCardContent, MatCardTitle } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatSpinner } from '@angular/material/progress-spinner';

@Component({
  standalone: true,
  selector: 'app-my-booking',
  templateUrl: './my-booking-component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatCard, MatCardTitle, MatCardContent, MatIcon, MatSpinner],
})
export class MyBookingComponent implements OnInit {
  myBookings: BookingDto[] = [];

  constructor(
    private bookingService: BookingService,
    private user: UserService,
    private cdr: ChangeDetectorRef,
  ) {}

  cancelBooking = (bookingId: number) => {
    this.bookingService.cancelBooking(bookingId).subscribe({
      next: () => {
        this.myBookings = this.myBookings.filter((booking) => booking.bookingId !== bookingId);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.log('Booking was not canceled');
      },
    });
  };
  ngOnInit(): void {
    this.bookingService.getMyBookings(this.user.getUser()?.userId).subscribe({
      next: (res: BookingDto[]) => {
        this.myBookings = res;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }
}
