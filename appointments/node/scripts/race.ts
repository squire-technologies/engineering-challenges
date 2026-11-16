// Fires concurrent bookings at the running API and checks that no two bookings overlap.
// It uses the "Race Test" staff member of Corner Barbers on a day one week from today.
// Usage: make race (the API must be running, see `make dev`).
import { addDays, toLocalDate, wallClockToUtc } from '../src/common/time';
import type { ShopWithCatalog } from '../src/shops/shops.types';
import type { ScheduleDay } from '../src/booking/booking.types';
import type { AppointmentResponse } from '../src/booking/dto/appointment.response';

const API = process.env.API_URL ?? 'http://localhost:3000';

type Booking = Pick<AppointmentResponse, 'id' | 'start' | 'end'>;

interface Result {
  status: number;
  booking?: Booking;
}

interface CheckResult {
  name: string;
  passed: boolean;
  detail: string;
}

async function request<T>(method: string, path: string, body?: object): Promise<{ status: number; json: T }> {
  const response = await fetch(`${API}${path}`, {
    method,
    headers: { 'content-type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  return { status: response.status, json: (await response.json()) as T };
}

function required<T>(value: T | undefined, what: string): T {
  if (value === undefined) {
    throw new Error(`${what} is missing from the seed data`);
  }
  return value;
}

async function main() {
  const { json: shops } = await request<ShopWithCatalog[]>('GET', '/shops');
  const shop = required(shops.find((s) => s.name === 'Corner Barbers'), 'Corner Barbers');
  const staff = required(shop.staff.find((s) => s.name === 'Race Test'), 'Race Test');
  const serviceId = (name: string) => required(shop.services.find((s) => s.name === name), name).id;
  const haircut = serviceId('Haircut');
  const color = serviceId('Color');
  const date = addDays(toLocalDate(new Date(), shop.timezone), 7);
  const at = (time: string) => wallClockToUtc(date, time, shop.timezone).toISOString();

  const book = async (service: string, start: string): Promise<Result> => {
    const { status, json } = await request<AppointmentResponse>('POST', '/appointments', {
      shopId: shop.id,
      serviceId: service,
      staffId: staff.id,
      start,
      customerName: 'Race',
      customerPhone: '+15550000000',
    });
    return { status, booking: status === 201 ? json : undefined };
  };
  const bookAll = (requests: [string, string][]) => Promise.all(requests.map(([service, start]) => book(service, start)));
  const times = (count: number, service: string, start: string): [string, string][] =>
    Array.from({ length: count }, () => [service, start]);

  await cancelEverythingOn(shop.id, staff.id, date);
  console.log(`Race Test, ${date} (${shop.timezone}), API ${API}\n`);
  const checks: CheckResult[] = [];

  // 1. Haircuts (30 min) at 10:00, 10:15, 10:30 and 10:45, ten requests each, all at once.
  const haircutRace = await bookAll(['10:00', '10:15', '10:30', '10:45'].flatMap((time) => times(10, haircut, at(time))));
  const raceWinners = winners(haircutRace);
  checks.push({
    name: 'Concurrent overlapping bookings',
    passed: raceWinners.length > 0 && noOverlaps(raceWinners) && onlyCreatedOrConflict(haircutRace),
    detail: `booked starts: ${startCounts(raceWinners)}; statuses ${statusCounts(haircutRace)}`,
  });

  // 2. Color is 15 min busy, 45 min free (processing), 20 min busy. One haircut fits in the free part.
  const colorBooking = await book(color, at('13:00'));
  const gapRace = await bookAll(times(20, haircut, at('13:15')));
  const gapWinners = winners(gapRace);
  checks.push({
    name: 'Processing time: one haircut fits inside Color',
    passed: colorBooking.status === 201 && gapWinners.length === 1 && onlyCreatedOrConflict(gapRace),
    detail: `Color at 13:00: ${colorBooking.status}; 20 haircuts at 13:15: ${statusCounts(gapRace)}`,
  });

  // 3. 13:30 overlaps the haircut booked at 13:15. 13:45 overlaps the rinse step of Color.
  const blocked = await bookAll([...times(5, haircut, at('13:30')), ...times(5, haircut, at('13:45'))]);
  checks.push({
    name: 'Busy time stays blocked',
    passed: winners(blocked).length === 0 && onlyCreatedOrConflict(blocked),
    detail: `5 haircuts at 13:30 and 5 at 13:45: ${statusCounts(blocked)}`,
  });

  // 4. Cancelling the 13:15 haircut frees the processing time again.
  const cancel = gapWinners[0] ? await request<AppointmentResponse>('POST', `/appointments/${gapWinners[0].id}/cancel`) : { status: 0 };
  const rebook = await book(haircut, at('13:30'));
  checks.push({
    name: 'Cancellation frees the time',
    passed: cancel.status === 200 && rebook.status === 201,
    detail: `cancel 13:15 haircut: ${cancel.status || 'skipped'}; haircut at 13:30: ${rebook.status}`,
  });

  for (const check of checks) {
    console.log(`${check.passed ? 'PASS' : 'FAIL'}  ${check.name}\n      ${check.detail}`);
  }
  // Overlapping rows left behind would break a later migration that adds a constraint.
  await cancelEverythingOn(shop.id, staff.id, date);
  const failed = checks.filter((check) => !check.passed).length;
  console.log(`\n${checks.length - failed} of ${checks.length} checks passed.`);
  process.exitCode = failed === 0 ? 0 : 1;
}

async function cancelEverythingOn(shopId: string, staffId: string, date: string) {
  const { json: schedule } = await request<ScheduleDay>('GET', `/shops/${shopId}/schedule?date=${date}`);
  const { appointments } = required(schedule.staff.find((s) => s.id === staffId), 'Race Test schedule');
  for (const appointment of appointments.filter((a) => a.status !== 'cancelled')) {
    await request('POST', `/appointments/${appointment.id}/cancel`);
  }
}

function winners(results: Result[]): Booking[] {
  return results.flatMap((result) => (result.booking ? [result.booking] : []));
}

function noOverlaps(bookings: Booking[]): boolean {
  return bookings.every((a, i) => bookings.slice(i + 1).every((b) => a.end <= b.start || b.end <= a.start));
}

function onlyCreatedOrConflict(results: Result[]): boolean {
  return results.every((result) => result.status === 201 || result.status === 409);
}

function statusCounts(results: Result[]): string {
  return countBy(results.map((result) => String(result.status)));
}

function startCounts(bookings: Booking[]): string {
  return countBy(bookings.map((booking) => booking.start)) || 'none';
}

function countBy(values: string[]): string {
  const counts = new Map<string, number>();
  values.forEach((value) => counts.set(value, (counts.get(value) ?? 0) + 1));
  return [...counts.entries()].map(([value, count]) => `${count}x ${value}`).join(', ');
}

main().catch((error) => {
  console.error(`Race check could not run: ${error.message}. Is the API running on ${API}?`);
  process.exitCode = 1;
});
