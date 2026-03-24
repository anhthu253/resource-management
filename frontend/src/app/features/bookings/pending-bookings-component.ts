import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  inject,
  OnInit,
} from '@angular/core';
import { BookingService } from '../../core/services/booking.service';
import { BookingDto } from '../../core/dtos/booking.dto';
import { UserService } from '../../core/services/user.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatIcon } from '@angular/material/icon';
import { MatSpinner } from '@angular/material/progress-spinner';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ConfirmDialog } from '../../core/components/pop-up/confirm-dialog-component';
import { MatDialog } from '@angular/material/dialog';

@Component({
  standalone: true,
  selector: 'app-pending-bookings',
  templateUrl: './pending-bookings-component.html',
  styleUrl: './pending-bookings-component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatIcon, MatSpinner],
})
export class PendingBookingsComponent implements OnInit {
  bookings: BookingDto[] = [];
  isLoading = false;
  message = '';
  private destroyRef = inject(DestroyRef);
  constructor(
    private router: Router,
    private dialog: MatDialog,
    private bookingService: BookingService,
    private userService: UserService,
    private cdr: ChangeDetectorRef,
  ) {}

  cancelBooking = (bookingId: number) => {
    const dialogRef = this.dialog
      .open(ConfirmDialog, {
        width: '350px',
        data: {
          message: 'Are you sure you want to cancel this booking?',
        },
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.bookingService.cancelBooking(bookingId).subscribe({
            next: () => {
              this.bookings = this.bookings.filter((booking) => booking.bookingId !== bookingId);
              this.cdr.detectChanges();
            },
            error: (err) => {
              console.log('Booking was not canceled');
            },
          });
        }
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
      this.isLoading = true;
      this.bookingService
        .getPendingBookings(userId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (res) => {
            this.isLoading = false;
            if (!res.length) this.message = 'You have no pending booking.';
            else this.message = '';
            this.bookings = res;
            this.cdr.detectChanges();
          },
          error: (err) => {
            this.isLoading = false;
            this.message =
              err.error ||
              err.message ||
              'Failed to fetch pending bookings. Please try again later.';
          },
        });
    }
  }
}
