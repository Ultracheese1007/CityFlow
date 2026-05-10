package com.cityflow.config.kafka;

import com.cityflow.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka 消费者错误处理配置：
 *   - 失败重试 3 次（每次间隔 1 秒）
 *   - 重试用尽后路由到死信 topic（DLT），由运维排查
 *
 * 这里把死信策略集中配置，consumer 方法体里只关心业务逻辑——
 * 抛 RuntimeException 即可，错误处理由这个 ErrorHandler 接管。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {

        // 1. DLT 路由器：失败消息 → seckill.order-created.DLT，保留原 key
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> {
                    log.error("Routing failed event to DLT: topic={} key={} error={}",
                            record.topic(), record.key(), ex.getMessage());
                    return new org.apache.kafka.common.TopicPartition(
                            KafkaTopics.SECKILL_ORDER_CREATED_DLT,
                            record.partition()
                    );
                }
        );

        // 2. retry：3 次，每次 1 秒
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        // 3. 组装：retry 用尽后调用 recoverer
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
