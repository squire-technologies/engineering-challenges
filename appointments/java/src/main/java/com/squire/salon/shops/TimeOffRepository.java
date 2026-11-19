package com.squire.salon.shops;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface TimeOffRepository extends JpaRepository<TimeOff, UUID> {

  @Query("""
      select t from TimeOff t
      where t.staffId in :staffIds and t.startAt < :to and t.endAt > :from
      order by t.startAt""")
  List<TimeOff> findOverlapping(Collection<UUID> staffIds, Instant from, Instant to);
}
