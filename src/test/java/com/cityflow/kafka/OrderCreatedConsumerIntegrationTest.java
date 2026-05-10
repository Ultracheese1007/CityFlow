package com.cityflow.kafka;

import com.cityflow.entity.SeckillVoucher;
import com.cityflow.entity.VoucherOrder;
import com.cityflow.repository.SeckillVoucherRepository;
import com.cityflow.repository.VoucherOrderRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Kafka 端到端集成测试——用 EmbeddedKafka 起一个进程内 broker，
 * 验证 producer/consumer 真实链路。
 *
 * 覆盖场景：
 *   1. happy path: producer 发消息，consumer 收到 + 写 DB
 *   2. 幂等: 同一 orderId 消费两次只入库一次
 *   3. DB 库存异常: consumer 抛错时不入库（DLT 验证留给独立 test）
 *
 * 注意：用 @MockBean 替换 Redis（StringRedisTemplate / RedissonClient）和
 * Repository——我们只测 Kafka 链路，不测 DB 真实行为。
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=cityflow-app-it",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
})
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = { KafkaTopics.SECKILL_ORDER_CREATED, KafkaTopics.SECKILL_ORDER_CREATED_DLT },
        brokerProperties = { "listeners=PLAINTEXT://localhost:0", "port=0" }
)
class OrderCreatedConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private VoucherOrderRepository voucherOrderRepository;

    @MockBean
    private SeckillVoucherRepository seckillVoucherRepository;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private RedissonClient redissonClient;

    @BeforeEach
    void resetMocks() {
        // 默认行为：订单不存在 + DB 扣库存成功
        when(voucherOrderRepository.existsById(anyLong())).thenReturn(false);
        when(seckillVoucherRepository.decreaseStock(anyLong())).thenReturn(1);
    }

    @Test
    @DisplayName("happy path：consumer 收到事件后扣库存 + 写订单")
    void happyPath_persistsOrderAndDecrementsStock() throws InterruptedException {
        OrderCreatedEvent event = new OrderCreatedEvent(
                100L, 1010L, 3L, LocalDateTime.now()
        );

        kafkaTemplate.send(KafkaTopics.SECKILL_ORDER_CREATED, "3", event);

        // consumer 异步处理——给它时间
        // verify(...).timeout(5000) 是 Mockito 提供的等待机制
        verify(seckillVoucherRepository, timeout(5000).times(1)).decreaseStock(3L);
        verify(voucherOrderRepository, timeout(5000).times(1)).save(argThat(o -> {
            VoucherOrder vo = (VoucherOrder) o;
            return vo.getId().equals(100L)
                    && vo.getUserId().equals(1010L)
                    && vo.getVoucherId().equals(3L)
                    && vo.getStatus().equals(1);
        }));
    }

    @Test
    @DisplayName("幂等：同一 orderId 已存在时跳过，不重复入库")
    void idempotent_existingOrderSkipped() {
        // 给定订单已经存在
        when(voucherOrderRepository.existsById(101L)).thenReturn(true);

        OrderCreatedEvent event = new OrderCreatedEvent(
                101L, 1010L, 3L, LocalDateTime.now()
        );
        kafkaTemplate.send(KafkaTopics.SECKILL_ORDER_CREATED, "3", event);

        // 等一会儿确认 consumer 真的处理了消息
        verify(voucherOrderRepository, timeout(5000).times(1)).existsById(101L);
        // 关键断言：扣库存和保存订单都不能发生
        verify(seckillVoucherRepository, never()).decreaseStock(anyLong());
        verify(voucherOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("DB 库存扣失败时跳过 save——不留下半完成订单")
    void stockDecrementFails_orderNotSaved() {
        when(seckillVoucherRepository.decreaseStock(anyLong())).thenReturn(0);

        OrderCreatedEvent event = new OrderCreatedEvent(
                102L, 1010L, 3L, LocalDateTime.now()
        );
        kafkaTemplate.send(KafkaTopics.SECKILL_ORDER_CREATED, "3", event);

        verify(seckillVoucherRepository, timeout(5000).times(1)).decreaseStock(3L);
        verify(voucherOrderRepository, never()).save(any());
    }
}
