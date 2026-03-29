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
import { FormsModule } from '@angular/forms';

@Component({
  standalone: true,
  selector: 'app-my-booking',
  templateUrl: './my-booking-component.html',
  styleUrl: './my-booking-component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatIcon, MatSpinner, MatTooltipModule, FormsModule],
})
export class MyBookingComponent implements OnInit {
  isLoading = false;
  bookings: (BookingDto & { isPastBooking: boolean })[] = [];
  allBookings: (BookingDto & { isPastBooking: boolean })[] = [];

  message = '';
  today = new Date().toISOString();
  searchTerm = '';
  sortDirection: 'asc' | 'desc' = 'asc';
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
          this.bookingService
            .cancelBooking(bookingId)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
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

  getUpdatedRefundStatus = (bookingId: number) => {
    this.bookingService
      .getCurrentBooking(bookingId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.bookings = this.bookings.map((booking) =>
            booking.bookingId === bookingId
              ? { ...booking, refundStatus: res.refundStatus }
              : booking,
          );
        },
        error: (err) => {
          this.dialog.open(NotificationDialog, {
            width: '350px',
            data: {
              message:
                err.err || err.message || 'Failed to fetch refund status. Please try again later.',
            },
          });
        },
      });
  };

  requestRefund = (booking: BookingDto) => {
    const dialogRef = this.dialog.open(NotificationDialog, {
      width: '350px',
      data: {
        message: 'Please wait...',
      },
    });
    this.bookingService
      .createRefund(booking.bookingId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res) => {
        //there is still a pending booking or unpaid booking that has been expired
        dialogRef.componentInstance.updateMessage(res.message);
        this.bookings = this.bookings.map((b) =>
          b.bookingId === booking.bookingId ? { ...b, refundStatus: res.status } : b,
        );
        this.cdr.detectChanges();
      });
    //track refund process
    this.refundService
      .getRefundStatusStream(booking.bookingId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res) => {
        dialogRef.componentInstance.updateMessage(res.message);
        this.bookings = this.bookings.map((b) =>
          b.bookingId === booking.bookingId ? { ...b, refundStatus: res.status } : b,
        );
        this.cdr.detectChanges();
      });
  };

  filterBookings = () => {
    const searchTerm = this.searchTerm.toLowerCase();
    this.bookings = this.allBookings.filter((booking) => {
      return (
        booking.bookingNumber.toLowerCase().includes(searchTerm) ||
        booking.bookingStatus.toLowerCase().includes(searchTerm) ||
        booking.refundStatus.toLowerCase().includes(searchTerm)
      );
    });
    this.cdr.detectChanges();
  };

  clearSearch = () => {
    this.searchTerm = '';
    this.bookings = this.allBookings;
  };

  toggleSort = () => {
    this.sortDirection = this.sortDirection === 'desc' ? 'asc' : 'desc';
    this.bookings = [...this.bookings].sort((a, b) => {
      const dateA = new Date(a.createdAt || '').getTime();
      const dateB = new Date(b.createdAt || '').getTime();
      return this.sortDirection === 'desc' ? dateB - dateA : dateA - dateB;
    });
    this.cdr.detectChanges();
  };

  ngOnInit(): void {
    this.isLoading = true;
    this.bookingService
      .getMyBookings(this.user.getUser()?.userId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: BookingDto[]) => {
          this.isLoading = false;
          if (!res.length) this.message = 'No booking found!';
          else this.message = '';
          this.allBookings = res.map((b) => {
            return {
              ...b,
              isPastBooking:
                !!b.startedAt && new Date(b.startedAt).getTime() < new Date().getTime(),
            };
          });
          this.bookings = this.allBookings;
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
