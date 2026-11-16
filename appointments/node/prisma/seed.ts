import 'dotenv/config';
import { PrismaPg } from '@prisma/adapter-pg';
import { Prisma, PrismaClient } from '../src/generated/prisma/client';
import { addDays, addMinutes, toLocalDate, wallClockToUtc, weekdayOf } from '../src/common/time';

const prisma = new PrismaClient({ adapter: new PrismaPg({ connectionString: process.env.DATABASE_URL }) });
const TIMEZONE = 'America/New_York';

// Deterministic random numbers, so every candidate gets the same data.
let state = 42;
function random(): number {
  state = (state + 0x6d2b79f5) | 0;
  let t = Math.imul(state ^ (state >>> 15), 1 | state);
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
}
const pick = <T>(items: T[]): T => items[Math.floor(random() * items.length)];

const FIRST_NAMES = ['Ava', 'Ben', 'Cleo', 'Dani', 'Eli', 'Fran', 'Gus', 'Hana', 'Ivy', 'Jules', 'Kai', 'Lena', 'Milo', 'Nora', 'Omar', 'Pia', 'Quinn', 'Rosa', 'Sol', 'Theo', 'Uma', 'Vic', 'Wren', 'Xan', 'Yara', 'Zoe', 'Ari', 'Bea', 'Cruz', 'Dina'];

async function main() {
  await prisma.$executeRawUnsafe('TRUNCATE shops, staff, working_hours, time_off, services, appointments CASCADE');
  const today = toLocalDate(new Date(), TIMEZONE);

  // Corner Barbers: small shop. "Race Test" works every day and is reserved for `make race`.
  const corner = await prisma.shop.create({ data: { name: 'Corner Barbers', timezone: TIMEZONE } });
  const cornerServices = await createServices(corner.id, [
    ['Haircut', 30, 3500],
    ['Beard Trim', 15, 2000],
    ['Color', 80, 9000],
  ]);
  const tuesdayToSaturday = [2, 3, 4, 5, 6];
  const alex = await createStaff(corner.id, 'Alex', tuesdayToSaturday, '09:00', '17:00');
  const sam = await createStaff(corner.id, 'Sam', tuesdayToSaturday, '10:00', '18:00');
  await createStaff(corner.id, 'Race Test', [0, 1, 2, 3, 4, 5, 6], '08:00', '20:00');
  const cornerAppointments = [alex, sam].flatMap((staff) => fillDays(corner.id, staff, cornerServices, addDays(today, 1), 14, 0.5));

  // Luxe Salon: large shop, used to feel the cost of availability queries.
  const luxe = await prisma.shop.create({ data: { name: 'Luxe Salon', timezone: TIMEZONE } });
  const luxeServices = await createServices(luxe.id, [
    ['Haircut', 45, 6500],
    ['Blowout', 30, 4500],
    ['Color', 80, 12000],
    ['Highlights', 120, 18000],
  ]);
  const mondayToSaturday = [1, 2, 3, 4, 5, 6];
  const luxeStaff: StaffSeed[] = [];
  for (const [index, name] of FIRST_NAMES.entries()) {
    const start = ['08:00', '09:00', '10:00'][index % 3];
    const end = ['17:00', '18:00', '19:00'][index % 3];
    luxeStaff.push(await createStaff(luxe.id, name, mondayToSaturday, start, end));
  }
  const luxeAppointments = luxeStaff.flatMap((staff) => fillDays(luxe.id, staff, luxeServices, addDays(today, -30), 90, 0.85));

  const lunchBreaks = luxeStaff.flatMap((staff) =>
    daysWorked(staff, addDays(today, -30), 90).map((date) => ({
      staffId: staff.id,
      startAt: wallClockToUtc(date, '13:00', TIMEZONE),
      endAt: wallClockToUtc(date, '13:30', TIMEZONE),
      reason: 'Lunch',
    })),
  );

  const appointments = [...cornerAppointments, ...luxeAppointments];
  for (let i = 0; i < appointments.length; i += 5000) {
    await prisma.appointment.createMany({ data: appointments.slice(i, i + 5000) });
  }
  await prisma.timeOff.createMany({ data: lunchBreaks });

  console.log(`Seeded 2 shops, ${luxeStaff.length + 3} staff, ${appointments.length} appointments, ${lunchBreaks.length} time-off rows.`);
}

interface StaffSeed {
  id: string;
  weekdays: number[];
  start: string;
  end: string;
}

interface ServiceSeed {
  id: string;
  durationMin: number;
}

async function createServices(shopId: string, rows: [string, number, number][]): Promise<ServiceSeed[]> {
  return Promise.all(
    rows.map(([name, durationMin, priceCents]) => prisma.service.create({ data: { shopId, name, durationMin, priceCents } })),
  );
}

async function createStaff(shopId: string, name: string, weekdays: number[], start: string, end: string): Promise<StaffSeed> {
  const staff = await prisma.staff.create({
    data: {
      shopId,
      name,
      workingHours: { create: weekdays.map((weekday) => ({ weekday, startTime: start, endTime: end })) },
    },
  });
  return { id: staff.id, weekdays, start, end };
}

function daysWorked(staff: StaffSeed, from: string, days: number): string[] {
  return Array.from({ length: days }, (_, i) => addDays(from, i)).filter((date) => staff.weekdays.includes(weekdayOf(date)));
}

/** Books back-to-back appointments with random gaps, leaving 13:00 to 13:30 free for lunch. */
function fillDays(shopId: string, staff: StaffSeed, services: ServiceSeed[], from: string, days: number, fillRate: number) {
  const rows: Prisma.AppointmentCreateManyInput[] = [];
  for (const date of daysWorked(staff, from, days)) {
    const close = wallClockToUtc(date, staff.end, TIMEZONE);
    const lunch = { start: wallClockToUtc(date, '13:00', TIMEZONE), end: wallClockToUtc(date, '13:30', TIMEZONE) };
    let cursor = wallClockToUtc(date, staff.start, TIMEZONE);
    while (cursor < close) {
      const service = pick(services);
      const end = addMinutes(cursor, service.durationMin);
      const hitsLunch = cursor < lunch.end && lunch.start < end;
      if (end > close) {
        break;
      }
      if (hitsLunch) {
        cursor = lunch.end;
        continue;
      }
      if (random() < fillRate) {
        rows.push({
          shopId,
          staffId: staff.id,
          serviceId: service.id,
          customerName: `Customer ${rows.length + 1}`,
          customerPhone: `+1555${String(Math.floor(random() * 1e7)).padStart(7, '0')}`,
          startAt: cursor,
          endAt: end,
          status: random() < 0.05 ? 'cancelled' : 'booked',
        });
        cursor = end;
      } else {
        cursor = addMinutes(cursor, pick([15, 30, 45]));
      }
    }
  }
  return rows;
}

main()
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  })
  .finally(() => prisma.$disconnect());
