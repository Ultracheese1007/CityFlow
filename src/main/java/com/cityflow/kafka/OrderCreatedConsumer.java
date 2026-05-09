package com.cityflow.kafka;

import com.cityflow.entity.VoucherOrder;
import com.cityflow.repository.SeckillVoucherRepository;
import com.cityflow.repository.VoucherOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 秒杀订单 DB 写入消费者。
 *
 * 幂等性：通过 existsById 检查避免重复入库——同一条事件被消费多次（Kafka at-least-once 语义）
 * 不会重复创建订单。
 *
 * 失败处理：方法抛异常时 spring-kafka 会按配置 retry，
 * 重试用尽后路由到 DLT（4E 完成时配置）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final VoucherOrderRepository voucherOrderRepository;
    private final SeckillVoucherRepository seckillVoucherRepository;

    @KafkaListener(topics = KafkaTopics.SECKILL_ORDER_CREATED, groupId = "cityflow-app")
    @Transactional(rollbackFor = Exception.class)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Consuming order event: orderId={} userId={} voucherId={}",
                event.getOrderId(), event.getUserId(), event.getVoucherId());

        // 1. 幂等检查——这条事件之前已经入过库，跳过
        if (voucherOrderRepository.existsById(event.getOrderId())) {
            log.info("Order {} already in DB, skipping (idempotent replay)", event.getOrderId());
            return;
        }

        // 2. DB 扣库存（带 stock>0 的 WHERE 防超卖）
        int updated = seckillVoucherRepository.decreaseStock(event.getVoucherId());
        if (updated == 0) {
            // Redis 跟 DB 状态不一致——理论上不应该发生（Redis 通过了 DB 应该也能扣）
            // 记 error 让运维排查；该订单不入库
            log.error("DB stock decrement failed for voucherId={} but Redis check passed. " +
                            "Inconsistency between Redis and DB. orderId={}",
                    event.getVoucherId(), event.getOrderId());
            return;
        }

        // 3. 写订单
        VoucherOrder order = new VoucherOrder();
        order.setId(event.getOrderId());
        order.setUserId(event.getUserId());
        order.setVoucherId(event.getVoucherId());
        order.setStatus(1);
        order.setPayType(1);
        voucherOrderRepository.save(order);

        log.info("Order {} persisted successfully", event.getOrderId());
    }
}
