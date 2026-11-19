package com.squire.salon.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BookingController {

  private final AppointmentsService appointments;
  private final AvailabilityService availability;
  private final ScheduleService schedule;

  BookingController(AppointmentsService appointments, AvailabilityService availability, ScheduleService schedule) {
    this.appointments = appointments;
    this.availability = availability;
    this.schedule = schedule;
  }

  /** {@code date} is the first local date in the shop's time zone. Leave out {@code staffId} to include all staff. */
  @GetMapping("/shops/{shopId}/availability")
  List<DayAvailability> getAvailability(
      @PathVariable UUID shopId,
      @RequestParam UUID serviceId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(defaultValue = "7") @Min(1) @Max(31) int days,
      @RequestParam(required = false) UUID staffId) {
    return availability.getAvailability(shopId, serviceId, date, days, staffId);
  }

  @GetMapping("/shops/{shopId}/schedule")
  ScheduleDay getSchedule(@PathVariable UUID shopId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return schedule.getDay(shopId, date);
  }

  @PostMapping("/appointments")
  @ResponseStatus(HttpStatus.CREATED)
  AppointmentResponse book(@Valid @RequestBody BookAppointmentRequest request) {
    return appointments.book(request);
  }

  @PostMapping("/appointments/{id}/cancel")
  AppointmentResponse cancel(@PathVariable UUID id) {
    return appointments.cancel(id);
  }
}
