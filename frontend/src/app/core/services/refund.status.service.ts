import { Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RefundStatus {
  bookingId: number;
  status: string;
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class RefundService {
  constructor(private zone: NgZone) {}

  getRefundStatusStream(bookingId: number): Observable<RefundStatus> {
    return new Observable<RefundStatus>((observer) => {
      const eventSource = new EventSource(
        `${environment.apiUrl}/booking/refund/status/${bookingId}`,
        { withCredentials: true },
      );

      eventSource.onmessage = (event) => {
        // Run inside Angular zone to trigger change detection
        this.zone.run(() => {
          observer.next(JSON.parse(event.data));
        });
      };

      eventSource.onerror = (error) => {
        this.zone.run(() => {
          observer.error(error);
        });
        eventSource.close();
      };

      // Cleanup when unsubscribed
      return () => {
        eventSource.close();
      };
    });
  }
}
