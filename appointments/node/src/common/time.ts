import { TZDate } from '@date-fns/tz';

/** A calendar date in a shop's time zone, formatted as YYYY-MM-DD. */
export type LocalDate = string;

export interface Interval {
  start: Date;
  end: Date;
}

/** Converts a wall-clock time on a local date to the UTC instant, honouring daylight saving time. */
export function wallClockToUtc(date: LocalDate, time: string, timezone: string): Date {
  const [year, month, day] = date.split('-').map(Number);
  const [hours, minutes] = time.split(':').map(Number);
  return new Date(new TZDate(year, month - 1, day, hours, minutes, timezone).getTime());
}

export function toLocalDate(instant: Date, timezone: string): LocalDate {
  const local = new TZDate(instant.getTime(), timezone);
  const month = String(local.getMonth() + 1).padStart(2, '0');
  const day = String(local.getDate()).padStart(2, '0');
  return `${local.getFullYear()}-${month}-${day}`;
}

/** 0 = Sunday ... 6 = Saturday */
export function weekdayOf(date: LocalDate): number {
  return new Date(`${date}T00:00:00Z`).getUTCDay();
}

export function addDays(date: LocalDate, days: number): LocalDate {
  const next = new Date(`${date}T00:00:00Z`);
  next.setUTCDate(next.getUTCDate() + days);
  return next.toISOString().slice(0, 10);
}

export function addMinutes(instant: Date, minutes: number): Date {
  return new Date(instant.getTime() + minutes * 60_000);
}

export function overlaps(a: Interval, b: Interval): boolean {
  return a.start < b.end && b.start < a.end;
}
