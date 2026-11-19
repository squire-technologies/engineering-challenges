package com.squire.salon.shops;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** A service a shop offers, such as a haircut. */
@Entity
@Table(name = "services")
public class BookableService {

  @Id
  private UUID id;
  private UUID shopId;
  private String name;
  private int durationMin;
  private int priceCents;

  protected BookableService() {}

  public UUID getId() {
    return id;
  }

  public UUID getShopId() {
    return shopId;
  }

  public String getName() {
    return name;
  }

  public int getDurationMin() {
    return durationMin;
  }

  public int getPriceCents() {
    return priceCents;
  }
}
