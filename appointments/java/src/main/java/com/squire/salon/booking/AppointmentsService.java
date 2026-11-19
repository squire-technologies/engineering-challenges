package com.squire.salon.booking;

import com.squire.salon.common.Interval;
import com.squire.salon.common.Times;
import com.squire.salon.shops.BookableService;
import com.squire.salon.shops.Shop;
import com.squire.salon.shops.ShopsService;
import com.squire.salon.shops.Staff;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppointmentsService {

  private final AppointmentRepository appointments;
  private final AvailabilityService availability;
  private final ShopsService shops;

  AppointmentsService(AppointmentRepository appointments, AvailabilityService availability, ShopsService shops) {
    this.appointments = appointments;
    this.availability = availability;
    this.shops = shops;
  }

  public AppointmentResponse book(BookAppointmentRequest request) {
    Shop shop = shops.getShop(request.shopId());
    BookableService service = shops.getService(shop.getId(), request.serviceId());
    Instant start = request.start();
    LocalDate date = Times.toLocalDate(start, shop.zone());

    UUID staffId = request.staffId() != null ? request.staffId() : firstFreeStaffId(request, date, start);
    Staff staff = shops.getStaffWithHours(shop.getId(), staffId).getFirst();
    if (!availability.freeStarts(shop, staff, service, date, Instant.now()).contains(start)) {
      throw notAvailable();
    }

    Interval time = new Interval(start, start.plusSeconds(service.getDurationMin() * 60L));
    Appointment appointment = Appointment.book(
        shop.getId(), staff.getId(), service.getId(), request.customerName(), request.customerPhone(), time);
    return AppointmentResponse.from(appointments.save(appointment));
  }

  public AppointmentResponse cancel(UUID id) {
    Appointment appointment = appointments.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
    if (appointment.getStatus() == AppointmentStatus.cancelled) {
      return AppointmentResponse.from(appointment);
    }
    appointment.cancel();
    return AppointmentResponse.from(appointments.save(appointment));
  }

  private UUID firstFreeStaffId(BookAppointmentRequest request, LocalDate date, Instant start) {
    String wanted = Times.iso(start);
    return availability.getAvailability(request.shopId(), request.serviceId(), date, 1, null).getFirst().slots().stream()
        .filter(slot -> slot.start().equals(wanted))
        .findFirst()
        .map(slot -> slot.staffIds().getFirst())
        .orElseThrow(AppointmentsService::notAvailable);
  }

  private static ResponseStatusException notAvailable() {
    return new ResponseStatusException(HttpStatus.CONFLICT, "This time is not available");
  }
}
