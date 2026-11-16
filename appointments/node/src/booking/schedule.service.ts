import { Injectable } from '@nestjs/common';
import { addDays, LocalDate, wallClockToUtc } from '../common/time';
import { ShopsService } from '../shops/shops.service';
import { workingInterval } from '../shops/working-hours';
import { AppointmentRepository } from './repositories/appointment.repository';
import { AppointmentResponse } from './dto/appointment.response';
import { ScheduleDay } from './booking.types';

@Injectable()
export class ScheduleService {
  constructor(
    private readonly shops: ShopsService,
    private readonly appointments: AppointmentRepository,
  ) {}

  /** One local day of a shop, per staff member: working hours, time off, and appointments. */
  async getDay(shopId: string, date: LocalDate): Promise<ScheduleDay> {
    const shop = await this.shops.getShop(shopId);
    const from = wallClockToUtc(date, '00:00', shop.timezone);
    const to = wallClockToUtc(addDays(date, 1), '00:00', shop.timezone);
    const staffList = await this.shops.getStaffWithHours(shop.id);
    const staffIds = staffList.map((staff) => staff.id);
    const [timeOff, appointments] = await Promise.all([
      this.shops.findTimeOff(staffIds, from, to),
      this.appointments.findWithServiceOverlapping(staffIds, from, to),
    ]);

    return {
      date,
      timezone: shop.timezone,
      staff: staffList.map((staff) => {
        const working = workingInterval(staff, date, shop.timezone);
        return {
          id: staff.id,
          name: staff.name,
          workingHours: working && { start: working.start.toISOString(), end: working.end.toISOString() },
          timeOff: timeOff
            .filter((t) => t.staffId === staff.id)
            .map((t) => ({ start: t.startAt.toISOString(), end: t.endAt.toISOString(), reason: t.reason })),
          appointments: appointments
            .filter((a) => a.staffId === staff.id)
            .map((a) => ({ ...AppointmentResponse.from(a), serviceName: a.service.name })),
        };
      }),
    };
  }
}
