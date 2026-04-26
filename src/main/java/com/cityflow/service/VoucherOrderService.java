package com.cityflow.service;

import com.cityflow.dto.Result;

/**
 * VoucherOrder 服务接口
 *
 * 只保留骨架，后续可添加业务方法
 */
public interface VoucherOrderService {

    Result seckillVoucher(Long voucherId);

    Result createVoucherOrder(Long voucherId);
}