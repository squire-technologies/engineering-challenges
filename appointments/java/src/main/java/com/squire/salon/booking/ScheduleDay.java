package com.squire.salon.booking;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** One local day of a shop, per staff member, for the staff day view. */
public record ScheduleDay(LocalDate date, String timezone, List<StaffDay> staff) {

  public record StaffDay(UUID id, String name, Range workingHours, List<TimeOffEntry> timeOff, List<ScheduledAppointment> appointments) {}

  public record Range(String start, String end) {}

  public record TimeOffEntry(String start, String end, String reason) {}

  public record ScheduledAppointment(@JsonUnwrapped AppointmentResponse appointment, String serviceName) {}
}
