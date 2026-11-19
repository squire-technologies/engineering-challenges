package com.squire.salon.booking;

import com.squire.salon.common.Interval;
import com.squire.salon.common.Times;
import com.squire.salon.shops.BookableService;
import com.squire.salon.shops.Shop;
import com.squire.salon.shops.ShopsService;
import com.squire.salon.shops.Staff;
import com.squire.salon.shops.TimeOff;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityService {

  private final ShopsService shops;
  private final AppointmentRepository appointments;

  AvailabilityService(ShopsService shops, AppointmentRepository appointments) {
    this.shops = shops;
    this.appointments = appointments;
  }

  /** Free start times for {@code days} days from {@code date}, for one staff member or, when null, all staff. */
  public List<DayAvailability> getAvailability(UUID shopId, UUID serviceId, LocalDate date, int days, UUID staffId) {
    Shop shop = shops.getShop(shopId);
    BookableService service = shops.getService(shop.getId(), serviceId);
    List<Staff> staffList = shops.getStaffWithHours(shop.getId(), staffId);

    Instant now = Instant.now();
    List<DayAvailability> result = new ArrayList<>();
    for (int i = 0; i < days; i++) {
      LocalDate day = date.plusDays(i);
      Map<Instant, List<UUID>> staffIdsByStart = new TreeMap<>();
      for (Staff staff : staffList) {
        for (Instant start : freeStarts(shop, staff, service, day, now)) {
          staffIdsByStart.computeIfAbsent(start, key -> new ArrayList<>()).add(staff.getId());
        }
      }
      List<DayAvailability.Slot> slots = staffIdsByStart.entrySet().stream()
          .map(entry -> new DayAvailability.Slot(Times.iso(entry.getKey()), entry.getValue()))
          .toList();
      result.add(new DayAvailability(day, slots));
    }
    return result;
  }

  /** Start times on {@code date} where {@code staff} can take {@code service}, stepping by the shop's slot interval. */
  List<Instant> freeStarts(Shop shop, Staff staff, BookableService service, LocalDate date, Instant now) {
    Interval working = staff.workingInterval(date, shop.zone()).orElse(null);
    if (working == null) {
      return List.of();
    }
    List<Interval> busy = Stream.concat(
            appointments.findOverlappingWithStatus(List.of(staff.getId()), AppointmentStatus.booked, working.start(), working.end())
                .stream()
                .map(Appointment::interval),
            shops.findTimeOff(List.of(staff.getId()), working).stream().map(TimeOff::interval))
        .toList();

    List<Instant> starts = new ArrayList<>();
    for (Instant start = working.start();
        !start.plusSeconds(service.getDurationMin() * 60L).isAfter(working.end());
        start = start.plusSeconds(shop.getSlotIntervalMin() * 60L)) {
      Interval candidate = new Interval(start, start.plusSeconds(service.getDurationMin() * 60L));
      if (!start.isBefore(now) && busy.stream().noneMatch(candidate::overlaps)) {
        starts.add(start);
      }
    }
    return starts;
  }
}
