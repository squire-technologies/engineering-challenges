package com.squire.salon.shops;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "shops")
public class Shop {

  @Id
  private UUID id;
  private String name;
  /** IANA time zone, e.g. America/New_York. Working hours are wall-clock times in this zone. */
  private String timezone;
  private int slotIntervalMin;

  protected Shop() {}

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getTimezone() {
    return timezone;
  }

  public ZoneId zone() {
    return ZoneId.of(timezone);
  }

  public int getSlotIntervalMin() {
    return slotIntervalMin;
  }
}
