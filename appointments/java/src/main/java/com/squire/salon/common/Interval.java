package com.squire.salon.common;

import java.time.Instant;

/** A half-open time range [start, end). */
public record Interval(Instant start, Instant end) {

  public boolean overlaps(Interval other) {
    return start.isBefore(other.end) && other.start.isBefore(end);
  }
}
