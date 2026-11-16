import { Matches } from 'class-validator';
import { LOCAL_DATE_PATTERN } from '../booking.constants';

export class ScheduleQueryDto {
  @Matches(LOCAL_DATE_PATTERN, { message: 'date must be YYYY-MM-DD' })
  date: string;
}
