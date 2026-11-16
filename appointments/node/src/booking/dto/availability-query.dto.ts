import { Type } from 'class-transformer';
import { IsInt, IsOptional, IsUUID, Matches, Max, Min } from 'class-validator';
import { DEFAULT_AVAILABILITY_DAYS, LOCAL_DATE_PATTERN, MAX_AVAILABILITY_DAYS } from '../booking.constants';

export class AvailabilityQueryDto {
  @IsUUID()
  serviceId: string;

  /** First local date, YYYY-MM-DD in the shop's time zone. */
  @Matches(LOCAL_DATE_PATTERN, { message: 'date must be YYYY-MM-DD' })
  date: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(MAX_AVAILABILITY_DAYS)
  days: number = DEFAULT_AVAILABILITY_DAYS;

  /** Leave out to include all staff of the shop. */
  @IsOptional()
  @IsUUID()
  staffId?: string;
}
