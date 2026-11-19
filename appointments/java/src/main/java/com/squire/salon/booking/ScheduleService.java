package com.squire.salon.booking;

import com.squire.salon.common.Interval;
import com.squire.salon.common.Times;
import com.squire.salon.shops.Shop;
import com.squire.salon.shops.ShopsService;
import com.squire.salon.shops.Staff;
import com.squire.salon.shops.TimeOff;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ScheduleService {

  private final ShopsService shops;
  private final AppointmentRepository appointments;

  ScheduleService(ShopsService shops, AppointmentRepository appointments) {
    this.shops = shops;
    this.appointments = appointments;
  }

  /** One local day of a shop, per staff member: working hours, time off, and appointments. */
  public ScheduleDay getDay(UUID shopId, LocalDate date) {
    Shop shop = shops.getShop(shopId);
    Interval day = new Interval(
        Times.wallClockToUtc(date, LocalTime.MIDNIGHT, shop.zone()),
        Times.wallClockToUtc(date.plusDays(1), LocalTime.MIDNIGHT, shop.zone()));
    List<Staff> staffList = shops.getStaffWithHours(shop.getId(), null);
    List<UUID> staffIds = staffList.stream().map(Staff::getId).toList();
    Map<UUID, String> serviceNames = shops.getServiceNames(shop.getId());
    Map<UUID, List<TimeOff>> timeOffByStaff = shops.findTimeOff(staffIds, day).stream()
        .collect(Collectors.groupingBy(TimeOff::getStaffId));
    Map<UUID, List<Appointment>> appointmentsByStaff = appointments.findOverlapping(staffIds, day.start(), day.end()).stream()
        .collect(Collectors.groupingBy(Appointment::getStaffId));

    List<ScheduleDay.StaffDay> staffDays = staffList.stream()
        .map(staff -> new ScheduleDay.StaffDay(
            staff.getId(),
            staff.getName(),
            staff.workingInterval(date, shop.zone())
                .map(hours -> new ScheduleDay.Range(Times.iso(hours.start()), Times.iso(hours.end())))
                .orElse(null),
            timeOffByStaff.getOrDefault(staff.getId(), List.of()).stream()
                .map(t -> new ScheduleDay.TimeOffEntry(Times.iso(t.interval().start()), Times.iso(t.interval().end()), t.getReason()))
                .toList(),
            appointmentsByStaff.getOrDefault(staff.getId(), List.of()).stream()
                .map(a -> new ScheduleDay.ScheduledAppointment(AppointmentResponse.from(a), serviceNames.get(a.getServiceId())))
                .toList()))
        .toList();
    return new ScheduleDay(date, shop.getTimezone(), staffDays);
  }
}
