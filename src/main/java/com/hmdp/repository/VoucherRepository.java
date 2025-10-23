package com.hmdp.repository;


import com.hmdp.dto.VoucherView;
import com.hmdp.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    @Query(value = """
        SELECT
          v.id                              AS id,
          v.shop_id                         AS shopId,
          v.title                           AS title,
          v.sub_title                       AS subTitle,
          v.rules                           AS rules,
          v.pay_value                       AS payValue,
          v.actual_value                    AS actualValue,
          v.type                            AS type,
          v.status                          AS status,
          v.create_time                     AS createTime,
          v.update_time                     AS updateTime,
          sv.stock                          AS stock,
          sv.begin_time                     AS beginTime,
          sv.end_time                       AS endTime
        FROM tb_voucher v
        LEFT JOIN tb_seckill_voucher sv ON v.id = sv.voucher_id
        WHERE v.shop_id = :shopId AND v.status = 1
        """, nativeQuery = true)
    List<VoucherView> queryVoucherOfShop(@Param("shopId") Long shopId);
}
