import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { BookingService } from '../../core/services/booking.service';
import { BookingDto } from '../../core/dtos/booking.dto';
import { CommonModule } from '@angular/common';
import { UserService } from '../../core/services/user.service';
import { MatCard, MatCardContent, MatCardTitle } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatSpinner } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { BookingStateService } from '../../core/services/booking.state.service';

@Component({
  standalone: true,
  selector: 'app-my-booking',
  templateUrl: './my-booking-component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatCard, MatCardTitle, MatCardContent, MatIcon, MatSpinner],
})
export class MyBookingComponent implements OnInit {
  bookings: (BookingDto & { isPastBooking: boolean })[] = [];
  today = new Date().toISOString();
  constructor(
    private router: Router,
    private bookingService: BookingService,
    private bookingStateService: BookingStateService,
    private user: UserService,
    private cdr: ChangeDetectorRef,
  ) {}

  cancelBooking = (bookingId: number) => {
    this.bookingService.cancelBooking(bookingId).subscribe({
      next: () => {
        this.bookings = this.bookings.filter((booking) => booking.bookingId !== bookingId);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.log('Booking was not canceled');
      },
    });
  };

  modifyBooking = (booking: BookingDto) => {
    this.bookingStateService.setBooking(booking);
    this.router.navigate(['/new-booking']);
  };

  ngOnInit(): void {
    this.bookingService.getMyBookings(this.user.getUser()?.userId).subscribe({
      next: (res: BookingDto[]) => {
        this.bookings = res.map((b) => {
          return {
            ...b,
            isPastBooking: !!b.startedAt && new Date(b.startedAt).getTime() < new Date().getTime(),
          };
        });
        this.cdr.detectChanges();
      },
      error: (err) => {},
    });
  }
}
