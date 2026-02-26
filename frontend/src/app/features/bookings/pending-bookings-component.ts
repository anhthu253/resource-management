import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { BookingService } from '../../core/services/booking.service';
import { BookingDto } from '../../core/dtos/booking.dto';
import { UserService } from '../../core/services/user.service';
import { BookingStateService } from '../../core/services/booking.state.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCard, MatCardContent, MatCardTitle } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatSpinner } from '@angular/material/progress-spinner';

@Component({
  standalone: true,
  selector: 'app-pending-bookings',
  templateUrl: './pending-bookings-component.html',
  styleUrl: './pending-bookings-component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatCard, MatCardTitle, MatCardContent, MatIcon, MatSpinner],
})
export class PendingBookingsComponent implements OnInit {
  bookings: BookingDto[] = [];
  constructor(
    private router: Router,
    private bookingService: BookingService,
    private userService: UserService,
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
    this.router.navigate(['/payment'], {
      queryParams: {
        bookingId: booking.bookingId,
        paymentId: booking.paymentId,
      },
    });
  };
  ngOnInit(): void {
    const userId = this.userService.getUser()?.userId;
    if (userId) {
      this.bookingService.getPendingBookings(userId).subscribe({
        next: (res) => {
          this.bookings = res;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.log(err.error);
        },
      });
    }
  }
}
