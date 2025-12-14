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
