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
import { MatDialog } from '@angular/material/dialog';
import { RefundService } from '../../core/services/refund.status.service';
import { NotificationDialog } from '../../core/components/pop-up/notification-component';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TmplAstRecursiveVisitor } from '@angular/compiler';

@Component({
  standalone: true,
  selector: 'app-my-booking',
  templateUrl: './my-booking-component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    MatCard,
    MatCardTitle,
    MatCardContent,
    MatIcon,
    MatSpinner,
    MatTooltipModule,
  ],
})
export class MyBookingComponent implements OnInit {
  isLoading = false;
  bookings: (BookingDto & { isPastBooking: boolean })[] = [];
  message = '';
  today = new Date().toISOString();
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

  openDialog = (message: string) => {
    const dialogRef = this.dialog.open(NotificationDialog, {
      width: '350px',
      data: {
        message: message,
      },
    });
  };

  trackRefund = (bookingId: number) => {
    this.refundService.getRefundStatusStream(bookingId).subscribe(
      (status) => {
        let message = 'We are working on it and will get back to you as soon as possible.';
        if (status) message = status.message;
        this.openDialog(message);
      },
      (error) => {
        console.log(error.err);
      },
    );
  };

  ngOnInit(): void {
    this.isLoading = true;
    this.bookingService.getMyBookings(this.user.getUser()?.userId).subscribe({
      next: (res: BookingDto[]) => {
        this.isLoading = false;
        if (!res.length) this.message = 'You have no booking confirmed.';
        else this.message = '';
        this.bookings = res.map((b) => {
          return {
            ...b,
            isPastBooking: !!b.startedAt && new Date(b.startedAt).getTime() < new Date().getTime(),
          };
        });
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
      },
    });
  }
}
