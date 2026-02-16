import { Injectable } from '@angular/core';
import { BookingResponseDto } from '../dtos/booking.dto';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class BookingStateService {
  private bookingDataSubject = new BehaviorSubject<BookingResponseDto | null>(null);
  bookingData$ = this.bookingDataSubject.asObservable();
  set(data: BookingResponseDto) {
    this.bookingDataSubject.next(data);
  }
  get(): BookingResponseDto | null {
    return this.bookingDataSubject.value;
  }
  clear() {
    this.bookingDataSubject.next(null);
  }
}
