import { Injectable } from '@angular/core';
import { BookingDto, BookingResponseDto } from '../dtos/booking.dto';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class BookingStateService {
  private bookingResponseSubject = new BehaviorSubject<BookingResponseDto | null>(null);
  bookingResponse$ = this.bookingResponseSubject.asObservable();
  setBookingResponse(data: BookingResponseDto) {
    this.bookingResponseSubject.next(data);
  }
  getBookingResponse(): BookingResponseDto | null {
    return this.bookingResponseSubject.value;
  }
  clearBookingResponse() {
    this.bookingResponseSubject.next(null);
  }

  private bookingSubject = new BehaviorSubject<BookingDto | null>(null);
  booking$ = this.bookingSubject.asObservable();
  setBooking = (booking: BookingDto) => {
    this.bookingSubject.next(booking);
  };
  getBooking = () => {
    return this.bookingSubject.value;
  };
  clearBooking = () => {
    this.bookingSubject.next(null);
  };
}
