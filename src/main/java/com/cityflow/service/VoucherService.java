package com.cityflow.service;

import com.cityflow.dto.Result;
import com.cityflow.entity.Voucher;

/**
 * Voucher 服务接口
 *
 * 保留业务方法，不再继承 MyBatis-Plus 的 IService
 */
public interface VoucherService {

    Long addVoucher(Voucher voucher);
    Result queryVoucherOfShop(Long shopId);
    void addSeckillVoucher(Voucher voucher);
}