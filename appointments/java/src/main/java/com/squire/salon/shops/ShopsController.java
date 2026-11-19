package com.squire.salon.shops;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ShopsController {

  private final ShopsService shops;

  ShopsController(ShopsService shops) {
    this.shops = shops;
  }

  @GetMapping("/shops")
  List<ShopCatalog> list() {
    return shops.listWithCatalog();
  }
}
