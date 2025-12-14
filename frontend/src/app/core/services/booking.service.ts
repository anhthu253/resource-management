import { Injectable } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { ResourceResponseDto } from '../dtos/resource.dto';
import { BookingRequestDto, BookingResponseDto } from '../dtos/booking.dto';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/internal/Observable';

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
}
