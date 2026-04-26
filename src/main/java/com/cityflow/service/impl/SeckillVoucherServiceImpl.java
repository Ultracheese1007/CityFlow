package com.cityflow.service.impl;

import com.cityflow.repository.SeckillVoucherRepository;
import com.cityflow.service.SeckillVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * SeckillVoucher 服务实现类
 *
 * 秒杀优惠券表，与优惠券是一对一关系
 * 只保留骨架，后续可实现业务逻辑
 */
@Service
@RequiredArgsConstructor
public class SeckillVoucherServiceImpl implements SeckillVoucherService {

    private final SeckillVoucherRepository seckillVoucherRepository;

    // 未来在这里实现业务逻辑
}