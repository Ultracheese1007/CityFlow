package com.cityflow.kafka;

/**
 * Kafka topic 名集中定义。所有 producer/consumer 用这里的常量，
 * 避免字符串散落在代码里出 typo。
 */
public final class KafkaTopics {

    /** 秒杀订单创建事件——producer 发，DB consumer 监听 */
    public static final String SECKILL_ORDER_CREATED = "seckill.order-created";

    /** 死信 topic——consumer 多次重试后失败的事件进这里给运维排查 */
    public static final String SECKILL_ORDER_CREATED_DLT = "seckill.order-created.DLT";

    private KafkaTopics() {}
}
