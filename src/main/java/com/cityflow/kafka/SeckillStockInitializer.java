package com.cityflow.kafka;

import com.cityflow.entity.SeckillVoucher;
import com.cityflow.repository.SeckillVoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.cityflow.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * 启动时把所有 SeckillVoucher 的库存预热到 Redis。
 *
 * 为什么 SETNX (setIfAbsent)：
 *   - 应用刚启动时 Redis 是空的，从 DB 复制
 *   - 重启时 Redis 还有上次的库存，那是"真相"（DB 还没消费完事件，是过期数据），不能覆盖
 *   - 用 SETNX 保证"只有 Redis 里没有时才填"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillStockInitializer {

    private final SeckillVoucherRepository seckillVoucherRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void preloadStock() {
        List<SeckillVoucher> vouchers = seckillVoucherRepository.findAll();
        int loaded = 0;
        int skipped = 0;

        for (SeckillVoucher v : vouchers) {
            String key = SECKILL_STOCK_KEY + v.getVoucherId();
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, String.valueOf(v.getStock()));
            if (Boolean.TRUE.equals(ok)) {
                loaded++;
            } else {
                skipped++;
            }
        }
        log.info("Seckill stock preloaded into Redis: loaded={} skipped(already-present)={}",
                loaded, skipped);
    }
}
