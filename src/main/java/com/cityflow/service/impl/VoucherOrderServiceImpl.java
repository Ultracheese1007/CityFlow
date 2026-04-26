package com.cityflow.service.impl;

import com.cityflow.dto.Result;
import com.cityflow.entity.SeckillVoucher;
import com.cityflow.entity.VoucherOrder;
import com.cityflow.repository.SeckillVoucherRepository;
import com.cityflow.repository.VoucherOrderRepository;
import com.cityflow.repository.VoucherRepository;
import com.cityflow.service.VoucherOrderService;
import com.cityflow.utils.RedisIdWorker;
import com.cityflow.utils.SimpleRedisLock;
import com.cityflow.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * VoucherOrder 服务实现类
 *
 * 只保留骨架，后续可实现业务逻辑
 */
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
            return Result.fail("秒杀券不存在");
        }
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀券尚未开始");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀券已结束");
        }
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足！");
        }

        Long userId = UserHolder.getUser().getId();
        //创建锁对象(新增代码)
        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        //获取锁对象
        boolean isLock = lock.tryLock(1200);
        //加锁失败
        if (!isLock) {
            return Result.fail("不允许重复下单");
        }
        try {
            //获取代理对象(事务)
            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            //释放锁
            lock.unlock();
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        boolean bought = voucherOrderRepository.existsByUserIdAndVoucherId(userId, voucherId);
        if (bought) {
            return Result.fail("用户已经购买过一次！");
        }

        boolean success = seckillVoucherRepository.decreaseStock(voucherId) > 0;
        if (!success) {
            return Result.fail("库存不足！");
        }

        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setStatus(1);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setPayType(1);
        voucherOrderRepository.save(voucherOrder);

        return Result.ok(orderId);
    }



}