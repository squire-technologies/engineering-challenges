package com.squire.salon.shops;

import com.squire.salon.common.Interval;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Blocked time for a staff member, in UTC. */
@Entity
@Table(name = "time_off")
public class TimeOff {

  @Id
  private UUID id;
  private UUID staffId;
  private Instant startAt;
  private Instant endAt;
  private String reason;

  protected TimeOff() {}

  public UUID getStaffId() {
    return staffId;
  }

  public Interval interval() {
    return new Interval(startAt, endAt);
  }

  public String getReason() {
    return reason;
  }
}
