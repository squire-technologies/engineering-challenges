package com.squire.salon.booking;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

  /** Appointments in {@code status} of these staff members that overlap [from, to). */
  @Query("""
      select a from Appointment a
      where a.staffId in :staffIds and a.status = :status and a.startAt < :to and a.endAt > :from""")
  List<Appointment> findOverlappingWithStatus(Collection<UUID> staffIds, AppointmentStatus status, Instant from, Instant to);

  /** Appointments in any status of these staff members that overlap [from, to). */
  @Query("""
      select a from Appointment a
      where a.staffId in :staffIds and a.startAt < :to and a.endAt > :from
      order by a.startAt""")
  List<Appointment> findOverlapping(Collection<UUID> staffIds, Instant from, Instant to);
}
