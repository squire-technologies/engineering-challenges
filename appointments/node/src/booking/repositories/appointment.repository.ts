import { Injectable } from '@nestjs/common';
import { Appointment, AppointmentStatus, Prisma } from '../../generated/prisma/client';
import { PrismaService } from '../../prisma/prisma.service';
import { AppointmentWithService } from '../booking.types';

@Injectable()
export class AppointmentRepository {
  constructor(private readonly prisma: PrismaService) {}

  create(data: Prisma.AppointmentUncheckedCreateInput): Promise<Appointment> {
    return this.prisma.appointment.create({ data });
  }

  findById(id: string): Promise<Appointment | null> {
    return this.prisma.appointment.findUnique({ where: { id } });
  }

  updateStatus(id: string, status: AppointmentStatus): Promise<Appointment> {
    return this.prisma.appointment.update({ where: { id }, data: { status } });
  }

  /** Booked appointments of these staff members that overlap [from, to). */
  findBookedOverlapping(staffIds: string[], from: Date, to: Date): Promise<Appointment[]> {
    return this.prisma.appointment.findMany({
      where: { staffId: { in: staffIds }, status: AppointmentStatus.booked, startAt: { lt: to }, endAt: { gt: from } },
    });
  }

  /** Appointments in any status that overlap [from, to), with the service name. */
  findWithServiceOverlapping(staffIds: string[], from: Date, to: Date): Promise<AppointmentWithService[]> {
    return this.prisma.appointment.findMany({
      where: { staffId: { in: staffIds }, startAt: { lt: to }, endAt: { gt: from } },
      include: { service: { select: { name: true } } },
      orderBy: { startAt: 'asc' },
    });
  }
}
