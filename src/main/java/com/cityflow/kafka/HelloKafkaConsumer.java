package com.cityflow.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Hello-world Kafka consumer——Phase 4B 阶段产物。
 * 监听 test.hello topic，把收到的消息打日志。
 *
 * 跟 producer 一样：4C 之后会被替换成 OrderEventConsumer。
 */
@Slf4j
@Component
public class HelloKafkaConsumer {

    @KafkaListener(topics = "test.hello", groupId = "cityflow-app")
    public void onMessage(ConsumerRecord<String, Map<String, Object>> record) {
        log.info("Kafka received: topic={} partition={} offset={} key={} value={}",
                record.topic(), record.partition(), record.offset(),
                record.key(), record.value());
    }
}
