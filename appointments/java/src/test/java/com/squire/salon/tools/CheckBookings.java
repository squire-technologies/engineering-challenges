package com.squire.salon.tools;

import com.squire.salon.common.Times;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fires concurrent bookings at the running API and checks that no two bookings overlap.
 * It uses the "Race Test" staff member of Corner Barbers on a day one week from today.
 * Usage: make check-bookings (the API must be running, see `make dev`).
 */
public class CheckBookings {

  private static final String API = System.getenv().getOrDefault("API_URL", "http://localhost:3000");
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final HttpClient HTTP = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  record Response(int status, JsonNode body) {}

  record Check(String name, boolean passed, String detail) {}

  private final JsonNode shop;
  private final String staffId;
  private final ZoneId zone;
  private final LocalDate date;

  CheckBookings(JsonNode shop, String staffId) {
    this.shop = shop;
    this.staffId = staffId;
    this.zone = ZoneId.of(shop.get("timezone").asString());
    this.date = LocalDate.now(zone).plusDays(7);
  }

  public static void main(String[] args) {
    int failed;
    try {
      JsonNode shop = findByName(request("GET", "/shops", null).body(), "Corner Barbers");
      String staffId = findByName(shop.get("staff"), "Race Test").get("id").asString();
      failed = new CheckBookings(shop, staffId).run();
    } catch (Exception error) {
      System.err.printf("Booking check could not run: %s. Is the API running on %s?%n", error.getMessage(), API);
      failed = 1;
    }
    System.exit(failed == 0 ? 0 : 1);
  }

  int run() {
    String haircut = serviceId("Haircut");
    String color = serviceId("Color");
    cancelEverything();
    System.out.printf("Race Test, %s (%s), API %s%n%n", date, zone, API);
    List<Check> checks = new ArrayList<>();

    // 1. Haircuts (30 min) at 10:00, 10:15, 10:30 and 10:45, ten requests each, all at once.
    List<Response> haircutRace = bookAll(Stream.of("10:00", "10:15", "10:30", "10:45")
        .flatMap(time -> times(10, haircut, at(time)).stream())
        .toList());
    List<JsonNode> raceWinners = winners(haircutRace);
    checks.add(new Check(
        "Concurrent overlapping bookings",
        !raceWinners.isEmpty() && noOverlaps(raceWinners) && onlyCreatedOrConflict(haircutRace),
        "booked starts: " + startCounts(raceWinners) + "; statuses " + statusCounts(haircutRace)));

    // 2. Color is 15 min busy, 45 min free (processing), 20 min busy. One haircut fits in the free part.
    Response colorBooking = book(color, at("13:00"));
    List<Response> gapRace = bookAll(times(20, haircut, at("13:15")));
    List<JsonNode> gapWinners = winners(gapRace);
    checks.add(new Check(
        "Processing time: one haircut fits inside Color",
        colorBooking.status() == 201 && gapWinners.size() == 1 && onlyCreatedOrConflict(gapRace),
        "Color at 13:00: " + colorBooking.status() + "; 20 haircuts at 13:15: " + statusCounts(gapRace)));

    // 3. 13:30 overlaps the haircut booked at 13:15. 13:45 overlaps the rinse step of Color.
    List<String[]> blockedRequests = new ArrayList<>(times(5, haircut, at("13:30")));
    blockedRequests.addAll(times(5, haircut, at("13:45")));
    List<Response> blocked = bookAll(blockedRequests);
    checks.add(new Check(
        "Busy time stays blocked",
        winners(blocked).isEmpty() && onlyCreatedOrConflict(blocked),
        "5 haircuts at 13:30 and 5 at 13:45: " + statusCounts(blocked)));

    // 4. Cancelling the 13:15 haircut frees the processing time again.
    int cancelStatus = gapWinners.isEmpty()
        ? 0
        : request("POST", "/appointments/" + gapWinners.getFirst().get("id").asString() + "/cancel", null).status();
    Response rebook = book(haircut, at("13:30"));
    checks.add(new Check(
        "Cancellation frees the time",
        cancelStatus == 200 && rebook.status() == 201,
        "cancel 13:15 haircut: " + (cancelStatus == 0 ? "skipped" : cancelStatus) + "; haircut at 13:30: " + rebook.status()));

    for (Check check : checks) {
      System.out.printf("%s  %s%n      %s%n", check.passed() ? "PASS" : "FAIL", check.name(), check.detail());
    }
    // Overlapping rows left behind would break a later migration that adds a constraint.
    cancelEverything();
    long failed = checks.stream().filter(check -> !check.passed()).count();
    System.out.printf("%n%d of %d checks passed.%n", checks.size() - failed, checks.size());
    return (int) failed;
  }

  private String serviceId(String name) {
    return findByName(shop.get("services"), name).get("id").asString();
  }

  private String at(String time) {
    return Times.iso(Times.wallClockToUtc(date, LocalTime.parse(time), zone));
  }

  private Response book(String serviceId, String start) {
    Map<String, String> body = Map.of(
        "shopId", shop.get("id").asString(),
        "serviceId", serviceId,
        "staffId", staffId,
        "start", start,
        "customerName", "Race",
        "customerPhone", "+15550000000");
    return request("POST", "/appointments", body);
  }

  /** Sends all requests at the same moment, one virtual thread each, whatever the number of CPU cores. */
  private List<Response> bookAll(List<String[]> requests) {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<CompletableFuture<Response>> pending = requests.stream()
          .map(r -> CompletableFuture.supplyAsync(() -> book(r[0], r[1]), executor))
          .toList();
      return pending.stream().map(CompletableFuture::join).toList();
    }
  }

  private static List<String[]> times(int count, String serviceId, String start) {
    return IntStream.range(0, count).mapToObj(i -> new String[] {serviceId, start}).toList();
  }

  private void cancelEverything() {
    JsonNode schedule = request("GET", "/shops/" + shop.get("id").asString() + "/schedule?date=" + date, null).body();
    for (JsonNode staff : schedule.get("staff")) {
      if (!staff.get("id").asString().equals(staffId)) {
        continue;
      }
      for (JsonNode appointment : staff.get("appointments")) {
        if (!appointment.get("status").asString().equals("cancelled")) {
          request("POST", "/appointments/" + appointment.get("id").asString() + "/cancel", null);
        }
      }
    }
  }

  private static Response request(String method, String path, Object body) {
    try {
      HttpRequest.BodyPublisher payload = body == null
          ? HttpRequest.BodyPublishers.noBody()
          : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body));
      HttpRequest request = HttpRequest.newBuilder(URI.create(API + path))
          .header("content-type", "application/json")
          .method(method, payload)
          .build();
      HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
      return new Response(response.statusCode(), JSON.readTree(response.body()));
    } catch (Exception error) {
      throw new IllegalStateException(method + " " + path + " failed: " + error.getMessage(), error);
    }
  }

  private static JsonNode findByName(JsonNode items, String name) {
    for (JsonNode item : items) {
      if (item.get("name").asString().equals(name)) {
        return item;
      }
    }
    throw new IllegalStateException(name + " is missing from the seed data");
  }

  private static List<JsonNode> winners(List<Response> responses) {
    return responses.stream().filter(r -> r.status() == 201).map(Response::body).toList();
  }

  private static boolean noOverlaps(List<JsonNode> bookings) {
    for (int i = 0; i < bookings.size(); i++) {
      for (int j = i + 1; j < bookings.size(); j++) {
        String aStart = bookings.get(i).get("start").asString();
        String aEnd = bookings.get(i).get("end").asString();
        String bStart = bookings.get(j).get("start").asString();
        String bEnd = bookings.get(j).get("end").asString();
        if (aStart.compareTo(bEnd) < 0 && bStart.compareTo(aEnd) < 0) {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean onlyCreatedOrConflict(List<Response> responses) {
    return responses.stream().allMatch(r -> r.status() == 201 || r.status() == 409);
  }

  private static String statusCounts(List<Response> responses) {
    return countBy(responses.stream().map(r -> String.valueOf(r.status())).toList());
  }

  private static String startCounts(List<JsonNode> bookings) {
    String counts = countBy(bookings.stream().map(b -> b.get("start").asString()).toList());
    return counts.isEmpty() ? "none" : counts;
  }

  private static String countBy(List<String> values) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    values.forEach(value -> counts.merge(value, 1, Integer::sum));
    return String.join(", ", counts.entrySet().stream().map(e -> e.getValue() + "x " + e.getKey()).toList());
  }
}
