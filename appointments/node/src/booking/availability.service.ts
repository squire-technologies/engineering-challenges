import { Injectable } from '@nestjs/common';
import { Service, Shop } from '../generated/prisma/client';
import { addDays, addMinutes, LocalDate, overlaps } from '../common/time';
import { ShopsService } from '../shops/shops.service';
import { StaffWithHours } from '../shops/shops.types';
import { workingInterval } from '../shops/working-hours';
import { AppointmentRepository } from './repositories/appointment.repository';
import { AvailabilityQueryDto } from './dto/availability-query.dto';
import { DayAvailability } from './booking.types';

@Injectable()
export class AvailabilityService {
  constructor(
    private readonly shops: ShopsService,
    private readonly appointments: AppointmentRepository,
  ) {}

  async getAvailability(shopId: string, query: AvailabilityQueryDto): Promise<DayAvailability[]> {
    const shop = await this.shops.getShop(shopId);
    const service = await this.shops.getService(shop.id, query.serviceId);
    const staffList = await this.shops.getStaffWithHours(shop.id, query.staffId);

    const now = new Date();
    const days: DayAvailability[] = [];
    for (let i = 0; i < query.days; i++) {
      const date = addDays(query.date, i);
      const staffIdsByStart = new Map<string, string[]>();
      for (const staff of staffList) {
        for (const start of await this.freeStarts(shop, staff, service, date, now)) {
          const key = start.toISOString();
          staffIdsByStart.set(key, [...(staffIdsByStart.get(key) ?? []), staff.id]);
        }
      }
      const slots = [...staffIdsByStart.entries()]
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([start, staffIds]) => ({ start, staffIds }));
      days.push({ date, slots });
    }
    return days;
  }

  /** Start times on `date` where `staff` can take `service`, stepping by the shop's slot interval. */
  async freeStarts(shop: Shop, staff: StaffWithHours, service: Service, date: LocalDate, now: Date): Promise<Date[]> {
    const working = workingInterval(staff, date, shop.timezone);
    if (!working) {
      return [];
    }
    const [appointments, timeOff] = await Promise.all([
      this.appointments.findBookedOverlapping([staff.id], working.start, working.end),
      this.shops.findTimeOff([staff.id], working.start, working.end),
    ]);
    const busy = [...appointments, ...timeOff].map((row) => ({ start: row.startAt, end: row.endAt }));

    const starts: Date[] = [];
    for (let start = working.start; addMinutes(start, service.durationMin) <= working.end; start = addMinutes(start, shop.slotIntervalMin)) {
      const candidate = { start, end: addMinutes(start, service.durationMin) };
      if (start >= now && !busy.some((interval) => overlaps(candidate, interval))) {
        starts.push(start);
      }
    }
    return starts;
  }
}
