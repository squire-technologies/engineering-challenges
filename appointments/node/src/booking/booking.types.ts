import { Prisma } from '../generated/prisma/client';
import { LocalDate } from '../common/time';
import { AppointmentResponse } from './dto/appointment.response';

export type AppointmentWithService = Prisma.AppointmentGetPayload<{ include: { service: { select: { name: true } } } }>;

export interface Slot {
  start: string;
  staffIds: string[];
}

export interface DayAvailability {
  date: LocalDate;
  slots: Slot[];
}

export interface ScheduleInterval {
  start: string;
  end: string;
}

export interface StaffSchedule {
  id: string;
  name: string;
  workingHours: ScheduleInterval | null;
  timeOff: (ScheduleInterval & { reason: string | null })[];
  appointments: (AppointmentResponse & { serviceName: string })[];
}

export interface ScheduleDay {
  date: LocalDate;
  timezone: string;
  staff: StaffSchedule[];
}
