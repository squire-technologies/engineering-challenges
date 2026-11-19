package com.squire.salon.shops;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface StaffRepository extends JpaRepository<Staff, UUID> {

  List<Staff> findAllByOrderByNameAsc();

  @EntityGraph(attributePaths = "workingHours")
  List<Staff> findWithHoursByShopIdOrderByNameAsc(UUID shopId);

  @EntityGraph(attributePaths = "workingHours")
  List<Staff> findWithHoursByShopIdAndId(UUID shopId, UUID id);
}
