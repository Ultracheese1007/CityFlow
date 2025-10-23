package com.hmdp.repository;

import com.hmdp.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    Page<Shop> findByTypeId(Long typeId, Pageable pageable);
    Page<Shop> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
