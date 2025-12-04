import { Injectable } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { ResourceResponseDto } from '../dtos/resource.dto';
import {
  BookingDto,
  BookingRequestDto,
  BookingResponseDto,
  PaymentIntentDto,
} from '../dtos/booking.dto';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/internal/Observable';
import { PaymentIntent } from '@stripe/stripe-js';

@Injectable({
  providedIn: 'root',
})
export class BookingService {
  constructor(private http: HttpClient) {}
  getResources = (startedAt: Date, endedAt: Date): Observable<ResourceResponseDto[]> => {
    return this.http.post<ResourceResponseDto[]>(
      'http://localhost:8080/booking/available-resources',
      { startedAt, endedAt },
      { withCredentials: true }
    );
  };

  createBooking = (bookingRequest: BookingRequestDto): Observable<BookingResponseDto> => {
    return this.http.post<BookingResponseDto>(
      'http://localhost:8080/booking/create',
      bookingRequest,
      {
        withCredentials: true,
      }
    );
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
}
