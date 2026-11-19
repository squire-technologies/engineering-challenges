package com.squire.salon.booking;

import com.squire.salon.common.Interval;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "appointments")
public class Appointment {

  @Id
  private UUID id;
  private UUID shopId;
  private UUID staffId;
  private UUID serviceId;
  private String customerName;
  private String customerPhone;
  private Instant startAt;
  private Instant endAt;

  @Enumerated(EnumType.STRING)
  private AppointmentStatus status;

  @CreationTimestamp
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  protected Appointment() {}

  static Appointment book(UUID shopId, UUID staffId, UUID serviceId, String customerName, String customerPhone, Interval time) {
    Appointment appointment = new Appointment();
    appointment.id = UUID.randomUUID();
    appointment.shopId = shopId;
    appointment.staffId = staffId;
    appointment.serviceId = serviceId;
    appointment.customerName = customerName;
    appointment.customerPhone = customerPhone;
    appointment.startAt = time.start();
    appointment.endAt = time.end();
    appointment.status = AppointmentStatus.booked;
    return appointment;
  }

  void cancel() {
    status = AppointmentStatus.cancelled;
  }

  public UUID getId() {
    return id;
  }

  public UUID getShopId() {
    return shopId;
  }

  public UUID getStaffId() {
    return staffId;
  }

  public UUID getServiceId() {
    return serviceId;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getCustomerPhone() {
    return customerPhone;
  }

  public Interval interval() {
    return new Interval(startAt, endAt);
  }

  public AppointmentStatus getStatus() {
    return status;
  }
}
