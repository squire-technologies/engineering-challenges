package com.squire.salon.shops;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "working_hours")
public class WorkingHours {

  @Id
  private UUID id;
  /** 0 = Sunday ... 6 = Saturday */
  private int weekday;
  /** "HH:MM", wall-clock time in the shop's time zone */
  private String startTime;
  private String endTime;

  protected WorkingHours() {}

  public int getWeekday() {
    return weekday;
  }

  public String getStartTime() {
    return startTime;
  }

  public String getEndTime() {
    return endTime;
  }
}
