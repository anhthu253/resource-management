import { Injectable } from '@angular/core';
import { ResourceDto } from '../dtos/resource.dto';
import { BookingDto, BookingResponseDto, PaymentIntentDto } from '../dtos/booking.dto';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/internal/Observable';
import { ConfigService } from './config.service';

@Injectable({
  providedIn: 'root',
})
export class BookingService {
  constructor(
    private http: HttpClient,
    private configService: ConfigService,
  ) {}

  getAllResources = (): Observable<ResourceDto[]> => {
    return this.http.get<ResourceDto[]>(`${this.configService.apiUrl}/booking/all-resources`, {
      withCredentials: true,
    });
  };

  getAvailableResources = (startedAt: Date, endedAt: Date): Observable<ResourceDto[]> => {
    return this.http.post<ResourceDto[]>(
      `${this.configService.apiUrl}/booking/available-resources`,
      { startedAt, endedAt },
      { withCredentials: true },
    );
  };

  createBooking = (bookingRequest: BookingDto): Observable<BookingDto> => {
    return this.http.post<BookingDto>(
      `${this.configService.apiUrl}/booking/create`,
      bookingRequest,
      {
        withCredentials: true,
      },
    );
  };

  updateBooking = (bookingId: number): Observable<string> => {
    return this.http.post<string>(`${this.configService.apiUrl}/booking/update`, bookingId, {
      withCredentials: true,
      responseType: 'text' as 'json',
    });
  };

  createRefund = (bookingId: number): Observable<{ status: string; message: string }> => {
    return this.http.post<{ status: string; message: string }>(
      `${this.configService.apiUrl}/booking/create-refund`,
      bookingId,
      {
        withCredentials: true,
      },
    );
  };

  cancelBooking = (bookingId: number): Observable<void> => {
    return this.http.post<void>(`${this.configService.apiUrl}/booking/cancel`, bookingId, {
      withCredentials: true,
    });
  };

  createPaymentIntent = (paymentIntent: PaymentIntentDto): Observable<string> => {
    return this.http.post<string>(
      `${this.configService.apiUrl}/booking/proceed-payment`,
      paymentIntent,
      {
        withCredentials: true,
        responseType: 'text' as 'json',
      },
    );
  };

  getTotalPrice = (booking: BookingDto): Observable<number> => {
    return this.http.post<number>(`${this.configService.apiUrl}/booking/total-price`, booking, {
      withCredentials: true,
    });
  };
  getCurrentBooking = (bookingId: number): Observable<BookingDto> => {
    return this.http.get<BookingDto>(
      `${this.configService.apiUrl}/booking/current-booking/${bookingId}`,
      {
        withCredentials: true,
      },
    );
  };

  getMyBookings = (userId?: number): Observable<BookingDto[]> => {
    return this.http.get<BookingDto[]>(
      `${this.configService.apiUrl}/booking/my-bookings/${userId}`,
      {
        withCredentials: true,
      },
    );
  };

  getPendingBookings = (userId?: number): Observable<BookingDto[]> => {
    return this.http.get<BookingDto[]>(
      `${this.configService.apiUrl}/booking/pending-bookings/${userId}`,
      {
        withCredentials: true,
      },
    );
  };
}
