import { ResourceDto } from './resource.dto';

export interface BookingResponseDto {
  bookingId: number;
  paymentId: number;
  paymentStatus: string;
}

export interface BookingDto {
  bookingId: number;
  bookingNumber: string;
  bookingStatus: string;
  modificationStatus: string;
  startedAt: Date | null;
  endedAt: Date | null;
  resources: ResourceDto[];
  totalPrice: number;
  userId: number;
}

export interface PaymentIntentDto {
  bookingId: number;
  paymentId: number;
  paymentMethodId: string;
}
