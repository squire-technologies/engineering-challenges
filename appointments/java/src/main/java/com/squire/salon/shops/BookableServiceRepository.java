package com.squire.salon.shops;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BookableServiceRepository extends JpaRepository<BookableService, UUID> {

  List<BookableService> findAllByOrderByNameAsc();

  List<BookableService> findByShopId(UUID shopId);

  Optional<BookableService> findByIdAndShopId(UUID id, UUID shopId);
}
