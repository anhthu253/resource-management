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
import { CommonModule } from '@angular/common';
import { UserService } from '../../core/services/user.service';
import { MatIcon } from '@angular/material/icon';
import { MatSpinner } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { BookingStateService } from '../../core/services/booking.state.service';
import { MatDialog } from '@angular/material/dialog';
import { RefundService } from '../../core/services/refund.status.service';
import { NotificationDialog } from '../../core/components/pop-up/notification-component';
import { ConfirmDialog } from '../../core/components/pop-up/confirm-dialog-component';
import { MatTooltipModule } from '@angular/material/tooltip';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  standalone: true,
  selector: 'app-my-booking',
  templateUrl: './my-booking-component.html',
  styleUrl: './my-booking-component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatIcon, MatSpinner, MatTooltipModule],
})
export class MyBookingComponent implements OnInit {
  isLoading = false;
  bookings: (BookingDto & { isPastBooking: boolean })[] = [];
  message = '';
  today = new Date().toISOString();
  private destroyRef = inject(DestroyRef);
  constructor(
    private router: Router,
    private bookingService: BookingService,
    private refundService: RefundService,
    private bookingStateService: BookingStateService,
    private user: UserService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog,
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
    this.bookingStateService.setBooking(booking);
    this.router.navigate(['/new-booking'], {
      state: { fromEdit: true },
    });
  };

  trackRefund = (bookingId: number) => {
    let message = 'Your refund request is being processed. You’ll see live updates here.';
    const dialogRef = this.dialog.open(NotificationDialog, {
      width: '350px',
      data: {
        message: message,
      },
    });
    const subscription = this.refundService.getRefundStatusStream(bookingId).subscribe(
      (res) => {
        this.bookings = this.bookings.map((booking) =>
          booking.bookingId === bookingId ? { ...booking, refundStatus: res.status } : booking,
        );
        dialogRef.componentInstance.updateMessage(res.message);
      },
      (error) => {
        dialogRef.componentInstance.updateMessage(
          error.err || error.message || 'Failed to fetch refund status. Please try again later.',
        );
      },
    );
    dialogRef.afterClosed().subscribe(() => {
      subscription.unsubscribe();
    });
  };

  createRefund = (bookingId: number) => {
    this.bookingService.createRefund(bookingId).subscribe(() => {
      console.log('Request a refund for booking ', bookingId);
    });
  };

  ngOnInit(): void {
    this.isLoading = true;
    this.bookingService
      .getMyBookings(this.user.getUser()?.userId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: BookingDto[]) => {
          this.isLoading = false;
          if (!res.length) this.message = 'You have no booking confirmed.';
          else this.message = '';
          this.bookings = res.map((b) => {
            return {
              ...b,
              isPastBooking:
                !!b.startedAt && new Date(b.startedAt).getTime() < new Date().getTime(),
            };
          });
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.isLoading = false;
          this.message =
            err.error || err.message || 'Failed to fetch your bookings. Please try again later.';
        },
      });
  }
}
