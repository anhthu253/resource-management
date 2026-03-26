import { ResourceDto } from './resource.dto';

export interface BookingResponseDto {
  bookingId: number;
  bookingNumber: string;
  paymentId: number;
}

export interface BookingDto {
  bookingId: number;
  bookingGroupId: number;
  bookingNumber: string;
  bookingStatus: string;
  paymentId: number;
  modificationStatus: string;
  refundStatus: string;
  startedAt: Date | null;
  endedAt: Date | null;
  createdAt: Date | null;
  resources: ResourceDto[];
  totalPrice: number;
  userId: number;
}

export interface PaymentIntentDto {
  bookingId: number;
  paymentId: number;
  paymentMethodId: string;
}
