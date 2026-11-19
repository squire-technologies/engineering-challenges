package com.squire.salon.shops;

import java.util.List;
import java.util.UUID;

/** A shop with its staff and services, for clients that start a booking. */
public record ShopCatalog(
    UUID id, String name, String timezone, int slotIntervalMin, List<StaffSummary> staff, List<ServiceSummary> services) {

  public record StaffSummary(UUID id, String name) {}

  public record ServiceSummary(UUID id, String name, int durationMin, int priceCents) {}
}
