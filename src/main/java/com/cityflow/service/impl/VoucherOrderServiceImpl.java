package com.cityflow.service.impl;

import com.cityflow.dto.ErrorCode;
import com.cityflow.dto.Result;
import com.cityflow.exception.BizException;
import com.cityflow.exception.NotFoundException;
import com.cityflow.kafka.KafkaTopics;
import com.cityflow.kafka.OrderCreatedEvent;
import com.cityflow.service.VoucherOrderService;
import com.cityflow.utils.RedisIdWorker;
import com.cityflow.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.cityflow.utils.RedisConstants.SECKILL_ORDER_KEY;
import static com.cityflow.utils.RedisConstants.SECKILL_STOCK_KEY;


/**
 * 秒杀订单服务——Phase 4 改造版。
 *
 * 设计：
 *   1. HTTP 链路只做 Redis 操作（库存 DECR + 已购集合 SADD），不碰 DB
 *   2. 通过则发 Kafka 事件，立即返回 orderId
 *   3. DB 写由 OrderCreatedConsumer 异步处理
 *
 * 取舍：
 *   - 没用 Lua 脚本——Java 多步 Redis 操作有微小竞态，但 DECR/SADD 都是原子的，
 *     失败回滚也明确。Phase 4D 可以升级到 Lua 一次性原子化。
 *   - 没在 createVoucherOrder 写 DB——consumer 接管那段逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherOrderServiceImpl implements VoucherOrderService {

    private final RedisIdWorker redisIdWorker;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();

        // 1. 检查库存是否存在（Redis 里没有 = voucher 不存在或没预热）
        String stockKey = SECKILL_STOCK_KEY + voucherId;
        String stockStr = stringRedisTemplate.opsForValue().get(stockKey);
        if (stockStr == null) {
            throw new NotFoundException(ErrorCode.VOUCHER_NOT_FOUND, "voucherId=" + voucherId);
        }

        // 2. 原子扣库存——返回值 < 0 说明刚才被并发抢光了，回滚
        Long remaining = stringRedisTemplate.opsForValue().decrement(stockKey);
        if (remaining == null || remaining < 0) {
            // 扣过了就要补回去（INCR 是 DECR 的逆操作）
            stringRedisTemplate.opsForValue().increment(stockKey);
            throw new BizException(ErrorCode.STOCK_INSUFFICIENT);
        }

        // 3. 标记用户已购——SADD 返回 0 表示这个 userId 已在集合里（重复购买）
        String orderKey = SECKILL_ORDER_KEY + voucherId;
        Long added = stringRedisTemplate.opsForSet().add(orderKey, userId.toString());
        if (added == null || added == 0L) {
            // 重复购买——把刚才扣的库存补回来
            stringRedisTemplate.opsForValue().increment(stockKey);
            throw new BizException(ErrorCode.ALREADY_PURCHASED);
        }

        // 4. 生成订单 ID + 发 Kafka 事件
        long orderId = redisIdWorker.nextId("order");
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, userId, voucherId, LocalDateTime.now()
        );
        kafkaTemplate.send(KafkaTopics.SECKILL_ORDER_CREATED, voucherId.toString(), event);

        log.info("Seckill order published: orderId={} userId={} voucherId={} stockRemaining={}",
                orderId, userId, voucherId, remaining);

        // 5. 立即返回——consumer 后台写库
        return Result.ok(orderId);
    }
}
