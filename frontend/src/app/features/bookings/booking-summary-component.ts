import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  NgZone,
  OnInit,
} from '@angular/core';
import { BookingService } from '../../core/services/booking.service';
import { BookingDto } from '../../core/dtos/booking.dto';
import { ActivatedRoute } from '@angular/router';
@Component({
  standalone: true,
  selector: 'app-booking-summary',
  templateUrl: './booking-summary-component.html',
  styleUrl: './booking-summary-component.css',
  imports: [CurrencyPipe, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingSummaryComponent implements OnInit {
  bookingDto: BookingDto | null = null;
  constructor(
    private bookingService: BookingService,
    private route: ActivatedRoute,
    private changeDetector: ChangeDetectorRef
  ) {}
  ngOnInit(): void {
    const bookingId = this.route.snapshot.params['bookingId'];
    this.bookingService.getCurrentBooking(bookingId).subscribe({
      next: (res) => {
        this.bookingDto = res;
        this.changeDetector.markForCheck();
      },
      error: (err) => console.log('error getting current booking'),
    });
  }
}
