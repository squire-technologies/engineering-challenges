package com.squire.salon.booking;

import com.squire.salon.common.Times;
import java.util.UUID;

public record AppointmentResponse(
    UUID id,
    UUID shopId,
    UUID staffId,
    UUID serviceId,
    String customerName,
    String customerPhone,
    String start,
    String end,
    AppointmentStatus status) {

  static AppointmentResponse from(Appointment appointment) {
    return new AppointmentResponse(
        appointment.getId(),
        appointment.getShopId(),
        appointment.getStaffId(),
        appointment.getServiceId(),
        appointment.getCustomerName(),
        appointment.getCustomerPhone(),
        Times.iso(appointment.interval().start()),
        Times.iso(appointment.interval().end()),
        appointment.getStatus());
  }
}
