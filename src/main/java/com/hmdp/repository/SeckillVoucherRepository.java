package com.hmdp.repository;

import com.hmdp.entity.SeckillVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeckillVoucherRepository extends JpaRepository<SeckillVoucher, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE tb_seckill_voucher " +
            "SET stock = stock - 1 " +
            "WHERE voucher_id = :id AND stock > 0", nativeQuery = true)
    int decrementStockIfEnough(@Param("id") Long voucherId);

    // 乐观锁
    @Modifying
    @Query("UPDATE SeckillVoucher v " +
            "SET v.stock = v.stock - 1 " +
            "WHERE v.voucherId = :voucherId AND v.stock > 0")
    int decreaseStock(@Param("voucherId") Long voucherId);
}