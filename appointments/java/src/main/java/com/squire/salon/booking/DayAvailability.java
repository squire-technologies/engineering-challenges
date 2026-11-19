package com.squire.salon.booking;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DayAvailability(LocalDate date, List<Slot> slots) {

  public record Slot(String start, List<UUID> staffIds) {}
}
