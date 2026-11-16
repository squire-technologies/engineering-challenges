import { IsISO8601, IsNotEmpty, IsOptional, IsString, IsUUID } from 'class-validator';

export class BookAppointmentDto {
  @IsUUID()
  shopId: string;

  @IsUUID()
  serviceId: string;

  /** Leave out to book any available staff member. */
  @IsOptional()
  @IsUUID()
  staffId?: string;

  /** ISO 8601 instant, for example 2026-12-01T15:00:00.000Z */
  @IsISO8601({ strict: true })
  start: string;

  @IsString()
  @IsNotEmpty()
  customerName: string;

  @IsString()
  @IsNotEmpty()
  customerPhone: string;
}
