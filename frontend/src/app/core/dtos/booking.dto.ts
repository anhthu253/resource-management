import { ResourceDto } from './resource.dto';

export interface BookingRequestDto {
  bookingId: number | null;
  resourceIds: number[];
  startedAt: Date | null;
  endedAt: Date | null;
  userId?: number;
  totalPrice: number;
}

export interface BookingResponseDto {
  bookingId: number;
  paymentId: number;
  paymentStatus: string;
}

export interface BookingDto {
  bookingId: number;
  status: string;
  startedAt: Date | null;
  endedAt: Date | null;
  resources: ResourceDto[];
  totalPrice: number;
}

export interface PaymentIntentDto {
  bookingId: number;
  paymentId: number;
  paymentMethodId: string;
}
