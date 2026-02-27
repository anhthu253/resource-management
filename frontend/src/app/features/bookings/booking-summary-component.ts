import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { BookingDto } from '../../core/dtos/booking.dto';
import { BookingService } from '../../core/services/booking.service';
import { ResourceDto } from '../../core/dtos/resource.dto';
import { ActivatedRoute, Route } from '@angular/router';
@Component({
  standalone: true,
  selector: 'app-booking-summary',
  templateUrl: './booking-summary-component.html',
  styleUrl: './booking-summary-component.css',
  imports: [CurrencyPipe, DatePipe, CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingSummaryComponent implements OnInit {
  booking: BookingDto | null = null;
  resources: ResourceDto[] = [];
  constructor(
    private bookingService: BookingService,
    private route: ActivatedRoute,
    private changeDetector: ChangeDetectorRef,
  ) {}
  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const bookingId = params.get('bookingId');
      if (bookingId) {
        this.bookingService.getCurrentBooking(+bookingId).subscribe({
          next: (res: BookingDto) => {
            this.booking = res;
            this.changeDetector.markForCheck();
          },
          error: (err) => console.log('error getting current booking'),
        });
      }
    });
  }
}
