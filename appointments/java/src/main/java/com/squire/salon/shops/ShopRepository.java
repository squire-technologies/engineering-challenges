package com.squire.salon.shops;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ShopRepository extends JpaRepository<Shop, UUID> {

  List<Shop> findAllByOrderByNameAsc();
}
