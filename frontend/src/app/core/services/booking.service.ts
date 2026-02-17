import { Injectable } from '@angular/core';
import { ResourceResponseDto } from '../dtos/resource.dto';
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

  getAllResources = (): Observable<ResourceResponseDto[]> => {
    return this.http.get<ResourceResponseDto[]>('http://localhost:8080/booking/all-resources', {
      withCredentials: true,
    });
  };

  getAvailableResources = (startedAt: Date, endedAt: Date): Observable<ResourceResponseDto[]> => {
    return this.http.post<ResourceResponseDto[]>(
      'http://localhost:8080/booking/available-resources',
      { startedAt, endedAt },
      { withCredentials: true },
    );
  };

  createBooking = (bookingRequest: BookingRequestDto): Observable<BookingResponseDto> => {
    return this.http.post<BookingResponseDto>(
      'http://localhost:8080/booking/create',
      bookingRequest,
      {
        withCredentials: true,
      },
    );
  };

  cancelBooking = (bookingId: number): Observable<void> => {
    return this.http.get<void>(`http://localhost:8080/booking/cancel-booking/${bookingId}`, {
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
