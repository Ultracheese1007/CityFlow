package com.cityflow.service.impl;

import com.cityflow.dto.ErrorCode;
import com.cityflow.dto.Result;
import com.cityflow.entity.SeckillVoucher;
import com.cityflow.entity.VoucherOrder;
import com.cityflow.exception.BizException;
import com.cityflow.exception.NotFoundException;
import com.cityflow.repository.SeckillVoucherRepository;
import com.cityflow.repository.VoucherOrderRepository;
import com.cityflow.repository.VoucherRepository;
import com.cityflow.service.VoucherOrderService;
import com.cityflow.utils.RedisIdWorker;
import com.cityflow.utils.SimpleRedisLock;
import com.cityflow.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 秒杀订单服务。
 *
 * 异常处理风格：
 *   - 业务校验失败统一抛 BizException / NotFoundException
 *   - WebExceptionAdvice 把异常转换成 Result.fail(ErrorCode)，
 *     带 HTTP 状态码 + 稳定的 API code 字段
 *   - 日志由 advice 统一打（warn 级别）；这里 service 层只在关键决策点打 info/debug
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherOrderServiceImpl implements VoucherOrderService {

    private final VoucherOrderRepository voucherOrderRepository;
    private final SeckillVoucherRepository seckillVoucherRepository;
    private final RedisIdWorker redisIdWorker;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher voucher = seckillVoucherRepository.findById(voucherId).orElse(null);
        if (voucher == null) {
            throw new NotFoundException(ErrorCode.VOUCHER_NOT_FOUND, "voucherId=" + voucherId);
        }
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.VOUCHER_NOT_STARTED);
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.VOUCHER_ENDED);
        }
        if (voucher.getStock() < 1) {
            throw new BizException(ErrorCode.STOCK_INSUFFICIENT);
        }

        Long userId = UserHolder.getUser().getId();
        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        boolean isLock = lock.tryLock(1200);
        if (!isLock) {
            log.warn("Failed to acquire seckill lock: userId={} voucherId={}", userId, voucherId);
            throw new BizException(ErrorCode.CONCURRENT_ORDER_DENIED);
        }
        try {
            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        boolean bought = voucherOrderRepository.existsByUserIdAndVoucherId(userId, voucherId);
        if (bought) {
            throw new BizException(ErrorCode.ALREADY_PURCHASED);
        }

        boolean success = seckillVoucherRepository.decreaseStock(voucherId) > 0;
        if (!success) {
            throw new BizException(ErrorCode.STOCK_INSUFFICIENT);
        }

        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setStatus(1);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setPayType(1);
        voucherOrderRepository.save(voucherOrder);

        log.info("Seckill order created: orderId={} userId={} voucherId={}",
                orderId, userId, voucherId);
        return Result.ok(orderId);
    }
}
