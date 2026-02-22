import { Injectable } from '@angular/core';
import { ResourceDto } from '../dtos/resource.dto';
import {
  BookingDto,
  BookingRequestDto,
  BookingResponseDto,
  PaymentIntentDto,
} from '../dtos/booking.dto';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/internal/Observable';

@Injectable({
  providedIn: 'root',
})
export class BookingService {
  constructor(private http: HttpClient) {}

  getAllResources = (): Observable<ResourceDto[]> => {
    return this.http.get<ResourceDto[]>('http://localhost:8080/booking/all-resources', {
      withCredentials: true,
    });
  };

  getAvailableResources = (startedAt: Date, endedAt: Date): Observable<ResourceDto[]> => {
    return this.http.post<ResourceDto[]>(
      'http://localhost:8080/booking/available-resources',
      { startedAt, endedAt },
      { withCredentials: true },
    );
  };

  createBooking = (bookingRequest: BookingRequestDto): Observable<BookingResponseDto> => {
    return this.http.post<BookingResponseDto>(
      'http://localhost:8080/booking/update',
      bookingRequest,
      {
        withCredentials: true,
      },
    );
  };

  cancelBooking = (bookingId: number): Observable<void> => {
    return this.http.post<void>('http://localhost:8080/booking/cancel', bookingId, {
      withCredentials: true,
    });
  };

  createPaymentIntent = (paymentIntent: PaymentIntentDto): Observable<string> => {
    return this.http.post<string>('http://localhost:8080/booking/proceed-payment', paymentIntent, {
      withCredentials: true,
      responseType: 'text' as 'json',
    });
  };

  getCurrentBooking = (bookingId: number): Observable<BookingDto> => {
    return this.http.get<BookingDto>(`http://localhost:8080/booking/current-booking/${bookingId}`, {
      withCredentials: true,
    });
  };

  getMyBookings = (userId?: number): Observable<BookingDto[]> => {
    return this.http.get<BookingDto[]>(`http://localhost:8080/booking/my-bookings/${userId}`, {
      withCredentials: true,
    });
  };
}
