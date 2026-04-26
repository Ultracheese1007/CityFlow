package com.cityflow.repository;

import com.cityflow.entity.VoucherOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherOrderRepository extends JpaRepository<VoucherOrder, Long> {
    boolean existsByUserIdAndVoucherId(Long userId, Long voucherId);
}