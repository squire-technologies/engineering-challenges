import { Interval, LocalDate, wallClockToUtc, weekdayOf } from '../common/time';
import { StaffWithHours } from './shops.types';

/** The UTC interval a staff member works on a local date, or null on a day off. */
export function workingInterval(staff: StaffWithHours, date: LocalDate, timezone: string): Interval | null {
  const hours = staff.workingHours.find((h) => h.weekday === weekdayOf(date));
  if (!hours) {
    return null;
  }
  return { start: wallClockToUtc(date, hours.startTime, timezone), end: wallClockToUtc(date, hours.endTime, timezone) };
}
