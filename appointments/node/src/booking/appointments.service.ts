import { ConflictException, Injectable, NotFoundException } from '@nestjs/common';
import { AppointmentStatus } from '../generated/prisma/client';
import { addMinutes, toLocalDate } from '../common/time';
import { ShopsService } from '../shops/shops.service';
import { AppointmentRepository } from './repositories/appointment.repository';
import { AvailabilityService } from './availability.service';
import { BookAppointmentDto } from './dto/book-appointment.dto';
import { AppointmentResponse } from './dto/appointment.response';

@Injectable()
export class AppointmentsService {
  constructor(
    private readonly appointments: AppointmentRepository,
    private readonly availability: AvailabilityService,
    private readonly shops: ShopsService,
  ) {}

  async book(dto: BookAppointmentDto): Promise<AppointmentResponse> {
    const shop = await this.shops.getShop(dto.shopId);
    const service = await this.shops.getService(shop.id, dto.serviceId);
    const start = new Date(dto.start);
    const date = toLocalDate(start, shop.timezone);

    const staffId = dto.staffId ?? (await this.firstFreeStaffId(dto, date, start));
    const [staff] = await this.shops.getStaffWithHours(shop.id, staffId);
    const freeStarts = await this.availability.freeStarts(shop, staff, service, date, new Date());
    if (!freeStarts.some((free) => free.getTime() === start.getTime())) {
      throw new ConflictException('This time is not available');
    }

    const appointment = await this.appointments.create({
      shopId: shop.id,
      staffId: staff.id,
      serviceId: service.id,
      customerName: dto.customerName,
      customerPhone: dto.customerPhone,
      startAt: start,
      endAt: addMinutes(start, service.durationMin),
    });
    return AppointmentResponse.from(appointment);
  }

  async cancel(id: string): Promise<AppointmentResponse> {
    const appointment = await this.appointments.findById(id);
    if (!appointment) {
      throw new NotFoundException('Appointment not found');
    }
    if (appointment.status === AppointmentStatus.cancelled) {
      return AppointmentResponse.from(appointment);
    }
    return AppointmentResponse.from(await this.appointments.updateStatus(id, AppointmentStatus.cancelled));
  }

  private async firstFreeStaffId(dto: BookAppointmentDto, date: string, start: Date): Promise<string> {
    const [day] = await this.availability.getAvailability(dto.shopId, { serviceId: dto.serviceId, date, days: 1 });
    const slot = day.slots.find((s) => s.start === start.toISOString());
    if (!slot) {
      throw new ConflictException('This time is not available');
    }
    return slot.staffIds[0];
  }
}
