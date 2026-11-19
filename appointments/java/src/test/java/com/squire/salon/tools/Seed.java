package com.squire.salon.tools;

import com.squire.salon.common.Times;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.flywaydb.core.Flyway;

/** Applies the migrations and loads the seed data. Usage: make seed */
public class Seed {

  private static final ZoneId ZONE = ZoneId.of("America/New_York");
  private static final List<String> FIRST_NAMES = List.of(
      "Ava", "Ben", "Cleo", "Dani", "Eli", "Fran", "Gus", "Hana", "Ivy", "Jules", "Kai", "Lena", "Milo", "Nora", "Omar",
      "Pia", "Quinn", "Rosa", "Sol", "Theo", "Uma", "Vic", "Wren", "Xan", "Yara", "Zoe", "Ari", "Bea", "Cruz", "Dina");

  record StaffSeed(UUID id, List<Integer> weekdays, LocalTime start, LocalTime end) {}

  record ServiceSeed(UUID id, int durationMin) {}

  record AppointmentRow(UUID shopId, UUID staffId, UUID serviceId, String customerName, String customerPhone,
                        Instant startAt, Instant endAt, String status) {}

  record TimeOffRow(UUID staffId, Instant startAt, Instant endAt, String reason) {}

  // Deterministic random numbers, the same sequence as the Node.js seed, so both challenges get the same data.
  private int state = 42;

  private final Connection db;

  Seed(Connection db) {
    this.db = db;
  }

  public static void main(String[] args) throws SQLException {
    String url = env("DATABASE_URL", "jdbc:postgresql://localhost:5432/salon");
    String user = env("DATABASE_USER", "salon");
    String password = env("DATABASE_PASSWORD", "salon");
    Flyway.configure().dataSource(url, user, password).load().migrate();
    try (Connection db = DriverManager.getConnection(url, user, password)) {
      db.setAutoCommit(false);
      new Seed(db).run();
      db.commit();
    }
  }

  void run() throws SQLException {
    execute("TRUNCATE shops, staff, working_hours, time_off, services, appointments CASCADE");
    LocalDate today = LocalDate.now(ZONE);

    // Corner Barbers: small shop. "Race Test" works every day and is reserved for `make check-bookings`.
    UUID corner = createShop("Corner Barbers");
    List<ServiceSeed> cornerServices = List.of(
        createService(corner, "Haircut", 30, 3500),
        createService(corner, "Beard Trim", 15, 2000),
        createService(corner, "Color", 80, 9000));
    List<Integer> tuesdayToSaturday = List.of(2, 3, 4, 5, 6);
    StaffSeed alex = createStaff(corner, "Alex", tuesdayToSaturday, "09:00", "17:00");
    StaffSeed sam = createStaff(corner, "Sam", tuesdayToSaturday, "10:00", "18:00");
    createStaff(corner, "Race Test", List.of(0, 1, 2, 3, 4, 5, 6), "08:00", "20:00");
    List<AppointmentRow> appointments = new ArrayList<>();
    for (StaffSeed staff : List.of(alex, sam)) {
      appointments.addAll(fillDays(corner, staff, cornerServices, today.plusDays(1), 14, 0.5));
    }

    // Luxe Salon: large shop, used to feel the cost of availability queries.
    UUID luxe = createShop("Luxe Salon");
    List<ServiceSeed> luxeServices = List.of(
        createService(luxe, "Haircut", 45, 6500),
        createService(luxe, "Blowout", 30, 4500),
        createService(luxe, "Color", 80, 12000),
        createService(luxe, "Highlights", 120, 18000));
    List<Integer> mondayToSaturday = List.of(1, 2, 3, 4, 5, 6);
    List<StaffSeed> luxeStaff = new ArrayList<>();
    for (int index = 0; index < FIRST_NAMES.size(); index++) {
      String start = List.of("08:00", "09:00", "10:00").get(index % 3);
      String end = List.of("17:00", "18:00", "19:00").get(index % 3);
      luxeStaff.add(createStaff(luxe, FIRST_NAMES.get(index), mondayToSaturday, start, end));
    }
    for (StaffSeed staff : luxeStaff) {
      appointments.addAll(fillDays(luxe, staff, luxeServices, today.minusDays(30), 90, 0.85));
    }

    List<TimeOffRow> lunchBreaks = new ArrayList<>();
    for (StaffSeed staff : luxeStaff) {
      for (LocalDate date : daysWorked(staff, today.minusDays(30), 90)) {
        lunchBreaks.add(new TimeOffRow(staff.id(), at(date, "13:00"), at(date, "13:30"), "Lunch"));
      }
    }

    insertAppointments(appointments);
    insertTimeOff(lunchBreaks);
    System.out.printf("Seeded 2 shops, %d staff, %d appointments, %d time-off rows.%n",
        luxeStaff.size() + 3, appointments.size(), lunchBreaks.size());
  }

  /** Books back-to-back appointments with random gaps, leaving 13:00 to 13:30 free for lunch. */
  List<AppointmentRow> fillDays(UUID shopId, StaffSeed staff, List<ServiceSeed> services, LocalDate from, int days, double fillRate) {
    List<AppointmentRow> rows = new ArrayList<>();
    for (LocalDate date : daysWorked(staff, from, days)) {
      Instant close = Times.wallClockToUtc(date, staff.end(), ZONE);
      Instant lunchStart = at(date, "13:00");
      Instant lunchEnd = at(date, "13:30");
      Instant cursor = Times.wallClockToUtc(date, staff.start(), ZONE);
      while (cursor.isBefore(close)) {
        ServiceSeed service = pick(services);
        Instant end = cursor.plusSeconds(service.durationMin() * 60L);
        boolean hitsLunch = cursor.isBefore(lunchEnd) && lunchStart.isBefore(end);
        if (end.isAfter(close)) {
          break;
        }
        if (hitsLunch) {
          cursor = lunchEnd;
          continue;
        }
        if (random() < fillRate) {
          String phone = "+1555" + String.format("%07d", (long) Math.floor(random() * 1e7));
          String status = random() < 0.05 ? "cancelled" : "booked";
          rows.add(new AppointmentRow(shopId, staff.id(), service.id(), "Customer " + (rows.size() + 1), phone, cursor, end, status));
          cursor = end;
        } else {
          cursor = cursor.plusSeconds(pick(List.of(15, 30, 45)) * 60L);
        }
      }
    }
    return rows;
  }

  List<LocalDate> daysWorked(StaffSeed staff, LocalDate from, int days) {
    return IntStream.range(0, days)
        .mapToObj(from::plusDays)
        .filter(date -> staff.weekdays().contains(Times.weekdayOf(date)))
        .toList();
  }

  double random() {
    state = state + 0x6d2b79f5;
    int t = (state ^ (state >>> 15)) * (1 | state);
    t = (t + (t ^ (t >>> 7)) * (61 | t)) ^ t;
    return Integer.toUnsignedLong(t ^ (t >>> 14)) / 4294967296.0;
  }

  <T> T pick(List<T> items) {
    return items.get((int) Math.floor(random() * items.size()));
  }

  private static Instant at(LocalDate date, String time) {
    return Times.wallClockToUtc(date, LocalTime.parse(time), ZONE);
  }

  private UUID createShop(String name) throws SQLException {
    UUID id = UUID.randomUUID();
    try (PreparedStatement insert = db.prepareStatement("INSERT INTO shops (id, name, timezone) VALUES (?, ?, ?)")) {
      insert.setObject(1, id);
      insert.setString(2, name);
      insert.setString(3, ZONE.getId());
      insert.executeUpdate();
    }
    return id;
  }

  private ServiceSeed createService(UUID shopId, String name, int durationMin, int priceCents) throws SQLException {
    UUID id = UUID.randomUUID();
    try (PreparedStatement insert = db.prepareStatement(
        "INSERT INTO services (id, shop_id, name, duration_min, price_cents) VALUES (?, ?, ?, ?, ?)")) {
      insert.setObject(1, id);
      insert.setObject(2, shopId);
      insert.setString(3, name);
      insert.setInt(4, durationMin);
      insert.setInt(5, priceCents);
      insert.executeUpdate();
    }
    return new ServiceSeed(id, durationMin);
  }

  private StaffSeed createStaff(UUID shopId, String name, List<Integer> weekdays, String start, String end) throws SQLException {
    UUID id = UUID.randomUUID();
    try (PreparedStatement insert = db.prepareStatement("INSERT INTO staff (id, shop_id, name) VALUES (?, ?, ?)")) {
      insert.setObject(1, id);
      insert.setObject(2, shopId);
      insert.setString(3, name);
      insert.executeUpdate();
    }
    try (PreparedStatement insert = db.prepareStatement(
        "INSERT INTO working_hours (id, staff_id, weekday, start_time, end_time) VALUES (?, ?, ?, ?, ?)")) {
      for (int weekday : weekdays) {
        insert.setObject(1, UUID.randomUUID());
        insert.setObject(2, id);
        insert.setInt(3, weekday);
        insert.setString(4, start);
        insert.setString(5, end);
        insert.addBatch();
      }
      insert.executeBatch();
    }
    return new StaffSeed(id, weekdays, LocalTime.parse(start), LocalTime.parse(end));
  }

  private void insertAppointments(List<AppointmentRow> rows) throws SQLException {
    try (PreparedStatement insert = db.prepareStatement("""
        INSERT INTO appointments (id, shop_id, staff_id, service_id, customer_name, customer_phone, start_at, end_at, status, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())""")) {
      for (AppointmentRow row : rows) {
        insert.setObject(1, UUID.randomUUID());
        insert.setObject(2, row.shopId());
        insert.setObject(3, row.staffId());
        insert.setObject(4, row.serviceId());
        insert.setString(5, row.customerName());
        insert.setString(6, row.customerPhone());
        insert.setObject(7, OffsetDateTime.ofInstant(row.startAt(), ZoneOffset.UTC));
        insert.setObject(8, OffsetDateTime.ofInstant(row.endAt(), ZoneOffset.UTC));
        insert.setString(9, row.status());
        insert.addBatch();
      }
      insert.executeBatch();
    }
  }

  private void insertTimeOff(List<TimeOffRow> rows) throws SQLException {
    try (PreparedStatement insert = db.prepareStatement(
        "INSERT INTO time_off (id, staff_id, start_at, end_at, reason) VALUES (?, ?, ?, ?, ?)")) {
      for (TimeOffRow row : rows) {
        insert.setObject(1, UUID.randomUUID());
        insert.setObject(2, row.staffId());
        insert.setObject(3, OffsetDateTime.ofInstant(row.startAt(), ZoneOffset.UTC));
        insert.setObject(4, OffsetDateTime.ofInstant(row.endAt(), ZoneOffset.UTC));
        insert.setString(5, row.reason());
        insert.addBatch();
      }
      insert.executeBatch();
    }
  }

  private void execute(String sql) throws SQLException {
    try (var statement = db.createStatement()) {
      statement.execute(sql);
    }
  }

  private static String env(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value;
  }
}
