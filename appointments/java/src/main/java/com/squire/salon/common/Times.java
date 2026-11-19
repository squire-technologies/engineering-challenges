package com.squire.salon.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class Times {

  private static final DateTimeFormatter ISO_MILLIS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  private Times() {}

  /** Converts a wall-clock time on a local date to the UTC instant, honouring daylight saving time. */
  public static Instant wallClockToUtc(LocalDate date, LocalTime time, ZoneId zone) {
    return ZonedDateTime.of(date, time, zone).toInstant();
  }

  public static LocalDate toLocalDate(Instant instant, ZoneId zone) {
    return instant.atZone(zone).toLocalDate();
  }

  /** 0 = Sunday ... 6 = Saturday */
  public static int weekdayOf(LocalDate date) {
    return date.getDayOfWeek().getValue() % 7;
  }

  /** ISO 8601 in UTC with milliseconds, for example 2026-12-01T15:00:00.000Z */
  public static String iso(Instant instant) {
    return ISO_MILLIS.format(instant);
  }
}
