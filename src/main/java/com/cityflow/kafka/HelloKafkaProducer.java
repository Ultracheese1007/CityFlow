package com.cityflow.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Hello-world Kafka producer——Phase 4B 阶段产物。
 * 应用启动后自动发一条消息到 test.hello topic，
 * 用来验证 Spring → Kafka 链路通畅。
 *
 * Phase 4C 接秒杀业务后，这个类会被替换成真实的 OrderEventProducer。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HelloKafkaProducer {

    private static final String TOPIC = "test.hello";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** 应用完全启动后自动发一条测试消息 */
    @EventListener(ApplicationReadyEvent.class)
    public void sendStartupPing() {
        Map<String, Object> payload = Map.of(
                "msg", "Spring app started, Kafka integration alive",
                "uuid", UUID.randomUUID().toString()
        );
        log.info("Sending startup ping to topic {}", TOPIC);
        kafkaTemplate.send(TOPIC, "startup", payload);
    }
}
