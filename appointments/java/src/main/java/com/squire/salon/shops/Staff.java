package com.squire.salon.shops;

import com.squire.salon.common.Interval;
import com.squire.salon.common.Times;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "staff")
public class Staff {

  @Id
  private UUID id;
  private UUID shopId;
  private String name;

  @OneToMany
  @JoinColumn(name = "staff_id")
  private List<WorkingHours> workingHours;

  protected Staff() {}

  public UUID getId() {
    return id;
  }

  public UUID getShopId() {
    return shopId;
  }

  public String getName() {
    return name;
  }

  /** The UTC interval this staff member works on a local date, or empty on a day off. */
  public Optional<Interval> workingInterval(LocalDate date, ZoneId zone) {
    int weekday = Times.weekdayOf(date);
    return workingHours.stream()
        .filter(hours -> hours.getWeekday() == weekday)
        .findFirst()
        .map(hours -> new Interval(
            Times.wallClockToUtc(date, LocalTime.parse(hours.getStartTime()), zone),
            Times.wallClockToUtc(date, LocalTime.parse(hours.getEndTime()), zone)));
  }
}
