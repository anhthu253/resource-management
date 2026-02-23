import { ResourceDto } from './resource.dto';
import { UserDto } from './user.dto';

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
  userId: number;
}

export interface PaymentIntentDto {
  bookingId: number;
  paymentId: number;
  paymentMethodId: string;
}
