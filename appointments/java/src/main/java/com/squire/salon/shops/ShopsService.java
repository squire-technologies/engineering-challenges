package com.squire.salon.shops;

import com.squire.salon.common.Interval;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** The only entry point to shop data for other modules. */
@Service
public class ShopsService {

  private final ShopRepository shops;
  private final StaffRepository staff;
  private final BookableServiceRepository services;
  private final TimeOffRepository timeOff;

  ShopsService(ShopRepository shops, StaffRepository staff, BookableServiceRepository services, TimeOffRepository timeOff) {
    this.shops = shops;
    this.staff = staff;
    this.services = services;
    this.timeOff = timeOff;
  }

  public List<ShopCatalog> listWithCatalog() {
    Map<UUID, List<Staff>> staffByShop =
        staff.findAllByOrderByNameAsc().stream().collect(Collectors.groupingBy(Staff::getShopId));
    Map<UUID, List<BookableService>> servicesByShop =
        services.findAllByOrderByNameAsc().stream().collect(Collectors.groupingBy(BookableService::getShopId));
    return shops.findAllByOrderByNameAsc().stream()
        .map(shop -> new ShopCatalog(
            shop.getId(),
            shop.getName(),
            shop.getTimezone(),
            shop.getSlotIntervalMin(),
            staffByShop.getOrDefault(shop.getId(), List.of()).stream()
                .map(s -> new ShopCatalog.StaffSummary(s.getId(), s.getName()))
                .toList(),
            servicesByShop.getOrDefault(shop.getId(), List.of()).stream()
                .map(s -> new ShopCatalog.ServiceSummary(s.getId(), s.getName(), s.getDurationMin(), s.getPriceCents()))
                .toList()))
        .toList();
  }

  public Shop getShop(UUID shopId) {
    return shops.findById(shopId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));
  }

  public BookableService getService(UUID shopId, UUID serviceId) {
    return services.findByIdAndShopId(serviceId, shopId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found in this shop"));
  }

  public Map<UUID, String> getServiceNames(UUID shopId) {
    return services.findByShopId(shopId).stream()
        .collect(Collectors.toMap(BookableService::getId, BookableService::getName));
  }

  /** All staff of the shop with their working hours, or only {@code staffId} when it is not null. */
  public List<Staff> getStaffWithHours(UUID shopId, UUID staffId) {
    if (staffId == null) {
      return staff.findWithHoursByShopIdOrderByNameAsc(shopId);
    }
    List<Staff> found = staff.findWithHoursByShopIdAndId(shopId, staffId);
    if (found.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found in this shop");
    }
    return found;
  }

  public List<TimeOff> findTimeOff(Collection<UUID> staffIds, Interval range) {
    return timeOff.findOverlapping(staffIds, range.start(), range.end());
  }
}
