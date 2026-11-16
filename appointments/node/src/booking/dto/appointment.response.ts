import { Appointment } from '../../generated/prisma/client';

export class AppointmentResponse {
  id: string;
  shopId: string;
  staffId: string;
  serviceId: string;
  customerName: string;
  customerPhone: string;
  start: string;
  end: string;
  status: string;

  static from(appointment: Appointment): AppointmentResponse {
    return {
      id: appointment.id,
      shopId: appointment.shopId,
      staffId: appointment.staffId,
      serviceId: appointment.serviceId,
      customerName: appointment.customerName,
      customerPhone: appointment.customerPhone,
      start: appointment.startAt.toISOString(),
      end: appointment.endAt.toISOString(),
      status: appointment.status,
    };
  }
}
