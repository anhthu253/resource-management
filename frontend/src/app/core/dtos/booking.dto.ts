export interface BookingRequestDto {
  resourceIds: number[];
  startedAt: Date | null;
  endedAt: Date | null;
  purpose: string;
  userId?: number;
  totalPrice: number;
}

export interface BookingResponseDto {
  bookingId: number;
  paymentId: number;
  paymentStatus: string;
}

export interface BookingDto {
  status: string;
  startedAt: Date | null;
  endedAt: Date | null;
  resources: string[];
  totalPrice: number;
}

export interface PaymentIntentDto {
  bookingId: number;
  paymentId: number;
  paymentMethodId: string;
}
